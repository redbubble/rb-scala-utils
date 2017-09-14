package com.redbubble.util.cache

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.util.concurrent.{Executor, TimeUnit}

import com.redbubble.util.async.syntax._
import com.redbubble.util.cache.CacheOps.sanitiseCacheName
import com.redbubble.util.metrics.StatsReceiver
import com.twitter.util.{Duration, Future}

import scala.concurrent.ExecutionContext.fromExecutor
import scala.concurrent.duration.{Duration => ScalaDuration}
import scalacache.serialization.{Codec, InMemoryRepr}
import scalacache.{Flags, ScalaCache}

sealed trait MemoryCache {
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

object MemoryCache {
  private val flags = Flags(readsEnabled = true, writesEnabled = true)

  def newCache(name: String, maxSize: Long, ttl: Duration)(implicit ex: Executor, statsReceiver: StatsReceiver): MemoryCache =
    new MemoryCache {
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
}

object RedisMemoryCache {
  private val flags = Flags(readsEnabled = true, writesEnabled = true)

  class RedisCodec[T] extends Codec[T, Array[Byte]] {
    override def serialize(value: T): Array[Byte] = {
      val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
      val oos = new ObjectOutputStream(stream)
      oos.writeObject(value)
      oos.close
      stream.toByteArray
    }

    override def deserialize(data: Array[Byte]): T = {
      val ois = new ObjectInputStream(new ByteArrayInputStream(data))
      val value = ois.readObject
      ois.close
      value.asInstanceOf[T]
    }
  }

//  class RedisCodec extends Codec[Any, Array[Byte]] with RedisSerialization

  def newCache(name: String, host: String, port: Int, ttl: Duration)(implicit ex: Executor, statsReceiver: StatsReceiver): MemoryCache =
    new MemoryCache {

      import scalacache._

      private val scalaTtl = ScalaDuration(ttl.inNanoseconds, TimeUnit.NANOSECONDS)
      private val cache: ScalaCache[Array[Byte]] = CacheOps.newRedisCache(name, host, port, scalaTtl, ex)
      private val ec = fromExecutor(ex)

      override def caching[V](key: CacheKey)(f: => Future[V]) = {
        val codec: Codec[V, Array[Byte]] = new RedisCodec[V]()
//        val codec = implicitly[Codec[V, Array[Byte]]]
        scalacache.cachingWithTTL[V, Array[Byte]](key)(scalaTtl)(f.asScala)(cache, flags, ec, codec).asTwitter(ec)
      }

      override def put[V, Repr](key: CacheKey, value: V): Future[Unit] = {
        val codec: Codec[V, Array[Byte]] = new RedisCodec[V]()
        scalacache.put[V, Array[Byte]](key)(value, Some(scalaTtl))(cache, flags, codec).asTwitter(ec)
      }

      override def get[V](key: CacheKey) = {
        val codec: Codec[V, Array[Byte]] = new RedisCodec[V]()
        scalacache.get[V, Array[Byte]](key)(cache, flags, codec).asTwitter(ec)
      }

      override def flush() = cache.cache.removeAll().asTwitter(ec)
    }
}
