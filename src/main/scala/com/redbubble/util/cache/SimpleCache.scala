package com.redbubble.util.cache

import java.util.concurrent.Executor

import com.redbubble.util.cache.memory.InMemorySimpleCache
import com.redbubble.util.cache.redis.RedisSimpleCache
import com.redbubble.util.metrics.StatsReceiver
import com.twitter.util.{Duration, Future}

object SimpleCache {
  def newMemoryCache(name: String, maxSize: Long, ttl: Duration)(implicit ex: Executor, statsReceiver: StatsReceiver): SimpleCache =
    new InMemorySimpleCache(sanitiseCacheName(name), maxSize, ttl)(ex, statsReceiver)

  def newRedisCache(name: String, host: String, port: Int, ttl: Duration)(implicit ex: Executor, statsReceiver: StatsReceiver): SimpleCache =
    new RedisSimpleCache(sanitiseCacheName(name), host, port, ttl)(ex, statsReceiver)

  private def sanitiseCacheName(n: String): String = n.replaceAll(" ", "_").toLowerCase
}

/**
  * A simple, asynchronous caching facade.
  */
trait SimpleCache {
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
