package com.redbubble.util.cache.memory

import java.util.concurrent.{Executor, TimeUnit}

import com.redbubble.util.async.syntax._
import com.redbubble.util.cache.{CacheKey, CacheOps, SimpleCache}
import com.redbubble.util.metrics.StatsReceiver
import com.twitter.util.{Duration, Future}

import scala.concurrent.ExecutionContext.fromExecutor
import scala.concurrent.duration.{Duration => ScalaDuration}
import scalacache.serialization.{Codec, InMemoryRepr}
import scalacache.{Flags, ScalaCache}

// TODO Can we de-duplicate this code with memory cache? Seems to be just the codec & underlying cache that is different.
private[cache] final class MemorySimpleCache(name: String, maxSize: Long, ttl: Duration)
    (implicit ex: Executor, statsReceiver: StatsReceiver) extends SimpleCache {
  private val flags = Flags(readsEnabled = true, writesEnabled = true)
  private val scalaTtl = ScalaDuration(ttl.inNanoseconds, TimeUnit.NANOSECONDS)
  private val cache: ScalaCache[InMemoryRepr] = CacheOps.newCache(name, maxSize, scalaTtl, ex)(statsReceiver)
  private val ec = fromExecutor(ex)

  statsReceiver.scope(name).addGauge("size")(estimatedSize.getOrElse(0L).toFloat)

  sys.addShutdownHook(cache.cache.close())

  override def caching[V](key: CacheKey)(f: => Future[V]): Future[V] = {
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

  override def flush(): Future[Unit] = cache.cache.removeAll().asTwitter(ec)

  private def estimatedSize = cache.cache match {
    case NonLoggingCaffeineCache(underlying) => Some(underlying.estimatedSize())
    case _ => None
  }
}
