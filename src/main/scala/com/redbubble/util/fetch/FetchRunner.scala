package com.redbubble.util.fetch

import com.redbubble.util.cache.{CacheKey, SimpleCache}
import com.redbubble.util.fetch.TwitterFutureFetchMonadError.twitterFutureFetchMonadError
import com.twitter.util.{Await, Duration, Future, FuturePool}
import fetch._

final case class FetchedObjectCache(underlyingCache: SimpleCache) extends DataSourceCache {
  private val WaitTimeout = Duration.fromSeconds(5)

  override def get[A](k: DataSourceIdentity): Option[A] = {
    // We rescue any failures from the underlying cache by returning None, which will cause a fetch from the datasource.
    val cacheGet = underlyingCache.get[A](CacheKey(k.toString)).rescue {
      case _ => Future.value(None)
    }
    Await.result(cacheGet, WaitTimeout)
  }

  override def update[A](k: DataSourceIdentity, v: A): FetchedObjectCache = {
    underlyingCache.put(CacheKey(k.toString()), v)
    this
  }
}

final case class FetcherRunner(c: SimpleCache)(implicit fp: FuturePool) {
  private val cache = FetchedObjectCache(c)

  /**
    * Runs this fetch using a `Future`, returning a `Future` containing the result.
    */
  def runFetchAsFuture[T](fetch: Fetch[T]): Future[T] = Fetch.run[Future](fetch, cache)(twitterFutureFetchMonadError)
}
