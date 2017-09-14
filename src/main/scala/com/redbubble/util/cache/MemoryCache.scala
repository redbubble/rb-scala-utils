package com.redbubble.util.cache

import java.io._
import java.util.concurrent.{Executor, TimeUnit}

import com.redbubble.util.async.syntax._
import com.redbubble.util.cache.CacheOps.sanitiseCacheName
import com.redbubble.util.metrics.StatsReceiver
import com.twitter.util.{Duration, Future}

import scala.concurrent.ExecutionContext.fromExecutor
import scala.concurrent.duration.{Duration => ScalaDuration}
import scala.reflect.ClassTag
import scalacache.serialization.{Codec, GZippingJavaAnyBinaryCodec, InMemoryRepr}
import scalacache.{Flags, ScalaCache}

object SimpleCache {
  def newMemoryCache(name: String, maxSize: Long, ttl: Duration)(implicit ex: Executor, statsReceiver: StatsReceiver): SimpleCache =
    new MemoryCache_(name, maxSize, ttl)(ex, statsReceiver)

  def newRedisCache(name: String, host: String, port: Int, ttl: Duration)(implicit ex: Executor, statsReceiver: StatsReceiver): SimpleCache =
    new RedisCache_(name, host, port, ttl)(ex, statsReceiver)
}

sealed trait SimpleCache {
  /**
    * Cache the result of executing `f` using the given `key`.
    */
  def caching[V](key: CacheKey)(f: => Future[V]): Future[V]

  /**
    * Manually put a value into the cache.
    *
    * Prefer `caching` instead of this function.
    */
  def put[V, Repr](key: CacheKey, value: V): Future[Unit]

  /**
    * Manually get a value from the cache.
    *
    * Prefer `caching` instead of this function.
    */
  def get[V](key: CacheKey): Future[Option[V]]

  /**
    * Flush (clear) the cache of all entries.
    */
  def flush(): Future[Unit]
}

final class MemoryCache_(name: String, maxSize: Long, ttl: Duration)(implicit ex: Executor, statsReceiver: StatsReceiver) extends SimpleCache {
  private val flags = Flags(readsEnabled = true, writesEnabled = true)
  private val scalaTtl = ScalaDuration(ttl.inNanoseconds, TimeUnit.NANOSECONDS)
  private val cache: ScalaCache[InMemoryRepr] = CacheOps.newCache(name, maxSize, scalaTtl, ex)(statsReceiver)
  private val ec = fromExecutor(ex)

  statsReceiver.scope(sanitiseCacheName(name)).addGauge("size")(estimatedSize.getOrElse(0L).toFloat)

  sys.addShutdownHook(cache.cache.close())

  override def caching[V](key: CacheKey)(f: => Future[V]) = {
    val noOpCodec = Codec.anyToNoSerialization[V]
    scalacache.cachingWithTTL[V, InMemoryRepr](key)(scalaTtl)(f.asScala)(cache, flags, ec, noOpCodec).asTwitter(ec)
  }

  override def put[V, Repr](key: CacheKey, value: V): Future[Unit] = {
    val noOpCodec = Codec.anyToNoSerialization[V]
    scalacache.put[V, InMemoryRepr](key)(value, Some(scalaTtl))(cache, flags, noOpCodec).asTwitter(ec)
  }

  override def get[V](key: CacheKey): Future[Option[V]] = {
    val noOpCodec = Codec.anyToNoSerialization[V]
    scalacache.get[V, InMemoryRepr](key)(cache, flags, noOpCodec).asTwitter(ec)
  }

  override def flush() = cache.cache.removeAll().asTwitter(ec)

  private def estimatedSize = cache.cache match {
    case NonLoggingCaffeineCache(underlying) => Some(underlying.estimatedSize())
    case _ => None
  }
}


final class RedisCache_(name: String, host: String, port: Int, ttl: Duration)(implicit ex: Executor, statsReceiver: StatsReceiver) extends SimpleCache {
  private val flags = Flags(readsEnabled = true, writesEnabled = true)
  private val scalaTtl = ScalaDuration(ttl.inNanoseconds, TimeUnit.NANOSECONDS)
  private val cache: ScalaCache[Array[Byte]] = CacheOps.newRedisCache(name, host, port, scalaTtl, ex)
  private val ec = fromExecutor(ex)

  override def caching[V](key: CacheKey)(f: => Future[V]) = {
    val codec = new MyCodec[V]
    scalacache.cachingWithTTL[V, Array[Byte]](key)(scalaTtl)(f.asScala)(cache, flags, ec, codec).asTwitter(ec)
  }

  override def put[V, Repr](key: CacheKey, value: V): Future[Unit] = {
    val codec = new MyCodec[V]
    scalacache.put[V, Array[Byte]](key)(value, Some(scalaTtl))(cache, flags, codec).asTwitter(ec)
  }

  override def get[V](key: CacheKey) = {
    val codec = new MyCodec[V]
    scalacache.get[V, Array[Byte]](key)(cache, flags, codec).asTwitter(ec)
  }

  override def flush() = cache.cache.removeAll().asTwitter(ec)
}

final class MyCodec[V] extends Codec[V, Array[Byte]] {

  implicit val classTag: ClassTag[V] = reflect.classTag[V]
  private val realCodec = GZippingJavaAnyBinaryCodec.default[Serializable]

  def serialize(value: V): Array[Byte] = {
    realCodec.serialize(value.asInstanceOf[Serializable])
  }

  def deserialize(data: Array[Byte]): V = {
    realCodec.deserialize(data).asInstanceOf[V]
  }
}
