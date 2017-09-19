package com.redbubble.util.cache.redis

import java.util.concurrent.Executor

import com.redbubble.util.cache.{CacheKey, Caching, SimpleCache}
import com.redbubble.util.metrics.StatsReceiver
import com.twitter.util.{Duration, Future}

import scalacache.{CacheConfig, ScalaCache}

/**
  * An external cache, backed by a Redis instance.
  *
  * @param name          The name of the cache, used for sending metrics with the cache name.
  * @param host          The hostname of the Redis instance.
  * @param port          The port of the Redis instance.
  * @param ttl           The time to live for items in the cache.
  * @param ex            The executor to use when performing async cache operations.
  * @param statsReceiver Where to log metrics to on the cache behaviour. Metrics are scoped by `name`.
  */
private[cache] final class RedisSimpleCache(name: String, host: String, port: Int, ttl: Duration)
    (implicit ex: Executor, statsReceiver: StatsReceiver) extends SimpleCache {
  private lazy val cache = createCache(name, host, port, ex, statsReceiver)

  override def caching[V](key: CacheKey)(f: => Future[V]): Future[V] = {
    val codec = new ScalaCacheExternaliserCodec[V]
    Caching.caching(cache, ttl, key, codec)(f)(ex)
  }

  override def put[V, Repr](key: CacheKey, value: V): Future[Unit] = {
    val codec = new ScalaCacheExternaliserCodec[V]
    Caching.put(cache, ttl, key, codec, value)(ex)
  }

  override def get[V](key: CacheKey): Future[Option[V]] = {
    val codec = new ScalaCacheExternaliserCodec[V]
    Caching.get(cache, key, codec)(ex)
  }

  override def flush(): Future[Unit] = Caching.flush(cache)

  private def createCache(name: String, host: String, port: Int, executor: Executor, statsReceiver: StatsReceiver): ScalaCache[Array[Byte]] = {
    val underlying = MetricsEnableRedisCache(name, host, port)(executor, statsReceiver)
    val c = ScalaCache(
      cache = underlying,
      cacheConfig = CacheConfig(Some(name))
    )
    sys.addShutdownHook(c.cache.close())
    c
  }
}


