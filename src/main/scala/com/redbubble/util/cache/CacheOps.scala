package com.redbubble.util.cache

import java.util.concurrent.Executor

import com.github.benmanes.caffeine.cache.Caffeine
import com.redbubble.util.cache.memory.{NonLoggingCaffeineCache, StatsCounter}
import com.redbubble.util.cache.redis.MetricsEnableRedisCache
import com.redbubble.util.metrics.StatsReceiver

import scala.concurrent.duration.Duration
import scalacache._
import scalacache.serialization.InMemoryRepr

private[cache] object CacheOps {
  /**
    * Create a new in-memory cache, with metrics tracking.
    *
    * @param name          The name of the cache, used for sending metrics with the cache name.
    * @param maxSize       The maximum number of entries the cache should hold.
    * @param ttl           The time to live for items in the cache.
    * @param executor      The executor to use when performing async cache operations.
    * @param statsReceiver Where to log metrics to on the cache behaviour. Metrics are scoped by `name`.
    */
  def newCache(name: String, maxSize: Long, ttl: Duration, executor: Executor)
      (implicit statsReceiver: StatsReceiver): ScalaCache[InMemoryRepr] = {
    val underlying = Caffeine.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(ttl.length, ttl.unit)
        .executor(executor)
        .recordStats(() => new StatsCounter(name, statsReceiver))
        .build[String, Object]
    ScalaCache(NonLoggingCaffeineCache(underlying))
  }

  /**
    * Create a new in-memory cache, with metrics tracking.
    *
    * @param name          The name of the cache, used for sending metrics with the cache name.
    * @param host          The hostname of the Redis instance.
    * @param port          The port of the Redis instance.
    * @param ttl           The time to live for items in the cache.
    * @param executor      The executor to use when performing async cache operations.
    * @param statsReceiver Where to log metrics to on the cache behaviour. Metrics are scoped by `name`.
    */
  def newRedisCache(name: String, host: String, port: Int, ttl: Duration, executor: Executor)
      (implicit statsReceiver: StatsReceiver): ScalaCache[Array[Byte]] = {
    // TODO Can we make use of the TTL?
    val underlying = MetricsEnableRedisCache(name, host, port)(executor, statsReceiver)
    ScalaCache[Array[Byte]](cache = underlying)
  }

  def sanitiseCacheName(n: String): String = n.replaceAll(" ", "_").toLowerCase
}
