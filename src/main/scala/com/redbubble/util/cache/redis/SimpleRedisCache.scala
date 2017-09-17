package com.redbubble.util.cache.redis

import java.util.concurrent.Executor

import com.redbubble.util.cache.CacheOps.sanitiseCacheName
import com.redbubble.util.metrics.StatsReceiver
import redis.clients.jedis._

import scala.concurrent.ExecutionContext.fromExecutor
import scala.concurrent.duration.Duration
import scalacache.redis.RedisCache

object SimpleRedisCache {
  def apply(name: String, host: String, port: Int)(implicit executor: Executor, statsReceiver: StatsReceiver): RedisCache =
    new SimpleRedisCache_(name, new JedisPool(host, port))(executor, statsReceiver)
}

private final class SimpleRedisCache_(name: String, override val jedisPool: JedisPool)
    (implicit val executor: Executor, statsReceiver: StatsReceiver)
    extends RedisCache(jedisPool, None, false)(fromExecutor(executor)) {

  statsReceiver.scope(sanitiseCacheName(name)).addGauge("size")(estimatedSize.getOrElse(0L).toFloat)

  override protected def logCacheHitOrMiss[A](key: String, result: Option[A]) = {
    super.logCacheHitOrMiss(key, result)
  }

  override protected def logCachePut(key: String, ttl: Option[Duration]) = {
    super.logCachePut(key, ttl)
  }
}
