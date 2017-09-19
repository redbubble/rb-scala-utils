package com.redbubble.util.cache.memory

import java.util.concurrent.{Executor, TimeUnit}

import com.github.benmanes.caffeine.cache.Caffeine
import com.redbubble.util.cache.{CacheKey, Caching, SimpleCache}
import com.redbubble.util.metrics.StatsReceiver
import com.twitter.util.{Duration, Future}

import scalacache.serialization.{Codec, InMemoryRepr}
import scalacache.{CacheConfig, ScalaCache}

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
  private val cache = createCache(name, maxSize, ex, statsReceiver)

  override def caching[V](key: CacheKey)(f: => Future[V]): Future[V] = {
    val codec = Codec.anyToNoSerialization[V]
    Caching.caching(cache, ttl, key, codec)(f)(ex)
  }

  override def put[V, Repr](key: CacheKey, value: V): Future[Unit] = {
    val codec = Codec.anyToNoSerialization[V]
    Caching.put(cache, ttl, key, codec, value)(ex)
  }

  override def get[V](key: CacheKey): Future[Option[V]] = {
    val codec = Codec.anyToNoSerialization[V]
    Caching.get(cache, key, codec)(ex)
  }

  override def flush(): Future[Unit] = Caching.flush(cache)(ex)

  private def createCache(
      name: String, maxSize: Long, executor: Executor, statsReceiver: StatsReceiver): ScalaCache[InMemoryRepr] = {
    val underlying = Caffeine.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(ttl.inNanoseconds, TimeUnit.NANOSECONDS)
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
