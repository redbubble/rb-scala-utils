package com.redbubble.util.cache.redis

import java.util.concurrent.Executor

import com.redbubble.util.metrics.StatsReceiver
import redis.clients.jedis._

import scala.concurrent.ExecutionContext.fromExecutor
import scala.concurrent.duration.Duration
import scalacache.redis.RedisCache

object MetricsEnableRedisCache {
  def apply(name: String, host: String, port: Int)(implicit executor: Executor, statsReceiver: StatsReceiver): RedisCache =
    new MetricsEnableRedisCache_(name, new JedisPool(host, port))(executor, statsReceiver)
}

/**
  * A simple wrapper around a Redis cache to add metrics support.
  */
//noinspection VarCouldBeVal
private final class MetricsEnableRedisCache_(cacheId: String, override val jedisPool: JedisPool)
    (implicit val executor: Executor, statsReceiver: StatsReceiver)
    extends RedisCache(jedisPool, None, false)(fromExecutor(executor)) {

  private var hitCount: Long = 0L
  private var missCount: Long = 0L
  private var putCount: Long = 0L

  private val stats = statsReceiver.scope(cacheId)
  private val hitsCounter = stats.counter("hits")
  private val missesCounter = stats.counter("misses")

  stats.provideGauge("hit_miss_ratio")(hitCount.toFloat / (hitCount.toFloat + missCount.toFloat))
  stats.provideGauge("miss_hit_ratio")(missCount.toFloat / (hitCount.toFloat + missCount.toFloat))
  stats.addGauge("size")(putCount.toFloat)

  override protected def logCacheHitOrMiss[A](key: String, result: Option[A]): Unit =
    result match {
      case None =>
        missCount = missCount + 1
        missesCounter.incr
      case Some(_) =>
        hitCount = hitCount + 1
        hitsCounter.incr
    }

  override protected def logCachePut(key: String, ttl: Option[Duration]): Unit = {
    putCount = putCount + 1
  }
}
