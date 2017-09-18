package com.redbubble.util.cache.redis

import java.util.concurrent.{Executor, TimeUnit}

import com.redbubble.util.async.syntax._
import com.redbubble.util.cache.{CacheKey, CacheOps, SimpleCache}
import com.redbubble.util.metrics.StatsReceiver
import com.twitter.util.{Duration, Future}

import scala.concurrent.ExecutionContext.fromExecutor
import scala.concurrent.duration.{Duration => ScalaDuration}
import scalacache.Flags

// TODO Can we de-duplicate this code with memory cache? Seems to be just the codec & underlying cache that is different.
private[cache] final class RedisSimpleCache(name: String, host: String, port: Int, ttl: Duration)
    (implicit ex: Executor, statsReceiver: StatsReceiver) extends SimpleCache {
  private val flags = Flags(readsEnabled = true, writesEnabled = true)
  private val scalaTtl = ScalaDuration(ttl.inNanoseconds, TimeUnit.NANOSECONDS)
  private val cache = CacheOps.newRedisCache(name, host, port, scalaTtl, ex)
  private val ec = fromExecutor(ex)

  // TODO Can we memoise/cache any of these little objects we're creating?
  override def caching[V](key: CacheKey)(f: => Future[V]): Future[V] = {
    val codec = new ScalaCacheExternaliserCodec[V]
    scalacache.cachingWithTTL[V, Array[Byte]](key)(scalaTtl)(f.asScala)(cache, flags, ec, codec).asTwitter(ec)
  }

  override def put[V, Repr](key: CacheKey, value: V): Future[Unit] = {
    val codec = new ScalaCacheExternaliserCodec[V]
    scalacache.put[V, Array[Byte]](key)(value, Some(scalaTtl))(cache, flags, codec).asTwitter(ec)
  }

  override def get[V](key: CacheKey): Future[Option[V]] = {
    val codec = new ScalaCacheExternaliserCodec[V]
    scalacache.get[V, Array[Byte]](key)(cache, flags, codec).asTwitter(ec)
  }

  override def flush(): Future[Unit] = cache.cache.removeAll().asTwitter(ec)
}


