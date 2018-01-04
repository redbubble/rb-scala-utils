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
  * @param executor      The executor to use when performing async cache operations.
  * @param statsReceiver Where to log metrics to on the cache behaviour. Metrics are scoped by `name`.
  */
private[cache] final class RedisSimpleCache(name: String, host: String, port: Int, ttl: Duration)
    (implicit override val executor: Executor, statsReceiver: StatsReceiver) extends SimpleCache {

  override protected type Repr = Array[Byte]
  override protected val underlying: ScalaCache[Repr] = createCache(name, host, port, executor, statsReceiver)

  override def caching[V](key: CacheKey)(f: => Future[V]): Future[V] = {
    val codec = new ScalaCacheExternaliserCodec[V]
    Caching.caching(underlying, ttl, key, codec)(f)(executor)
  }

  override def put[V](key: CacheKey, value: V): Future[Any] = {
    val codec = new ScalaCacheExternaliserCodec[V]
    Caching.put(underlying, ttl, key, codec, value)(executor)
  }

  override def get[V](key: CacheKey): Future[Option[V]] = {
    val codec = new ScalaCacheExternaliserCodec[V]
    Caching.get(underlying, key, codec)(executor)
  }

  private def createCache(name: String, host: String, port: Int, executor: Executor, statsReceiver: StatsReceiver): ScalaCache[Repr] = {
    val underlying = MetricsEnableRedisCache(name, host, port)(executor, statsReceiver)
    val c = ScalaCache(
      cache = underlying,
      cacheConfig = CacheConfig(Some(name))
    )
    sys.addShutdownHook(c.cache.close())
    c
  }
}


