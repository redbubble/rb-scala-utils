package com.redbubble.util.cache.memory

import java.util.concurrent.{Executor, TimeUnit}

import com.github.benmanes.caffeine.cache.Caffeine
import com.redbubble.util.async.syntax._
import com.redbubble.util.cache.{CacheKey, SimpleCache}
import com.redbubble.util.metrics.StatsReceiver
import com.twitter.util.{Duration, Future}

import scala.concurrent.ExecutionContext.fromExecutor
import scala.concurrent.duration.{Duration => ScalaDuration}
import scalacache.serialization.{Codec, InMemoryRepr}
import scalacache.{CacheConfig, Flags, ScalaCache}

/**
  * An in-memory cache, with metrics tracking.
  *
  * @param name          The name of the cache, used for sending metrics with the cache name.
  * @param maxSize       The maximum number of entries the cache should hold.
  * @param ttl           The time to live for items in the cache.
  * @param ex            The executor to use when performing async cache operations.
  * @param statsReceiver Where to log metrics to on the cache behaviour. Metrics are scoped by `name`.
  */
private[cache] final class InMemorySimpleCache(name: String, maxSize: Long, ttl: Duration)
    (implicit ex: Executor, statsReceiver: StatsReceiver) extends SimpleCache {
  private val flags = Flags(readsEnabled = true, writesEnabled = true)
  private val scalaTtl = ScalaDuration(ttl.inNanoseconds, TimeUnit.NANOSECONDS)
  private val cache = createCache(name, maxSize, scalaTtl, ex, statsReceiver)
  private val ec = fromExecutor(ex)

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

  private def createCache(
      name: String, maxSize: Long, ttl: ScalaDuration, executor: Executor, statsReceiver: StatsReceiver) = {
    val underlying = Caffeine.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(ttl.length, ttl.unit)
        .executor(executor)
        .recordStats(() => new StatsCounter(name, statsReceiver))
        .build[String, Object]
    statsReceiver.scope(name).addGauge("size")(underlying.estimatedSize().toFloat)
    val c = ScalaCache(
      cache = NonLoggingCaffeineCache(underlying),
      cacheConfig = CacheConfig(Some(name))
    )
    sys.addShutdownHook(c.cache.close())
    c
  }
}
