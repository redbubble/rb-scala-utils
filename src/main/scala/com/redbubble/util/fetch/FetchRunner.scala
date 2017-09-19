package com.redbubble.util.fetch

import cats.syntax.either._
import com.redbubble.util.cache.{CacheKey, SimpleCache}
import com.redbubble.util.fetch.TwitterFutureFetchMonadError.twitterFutureFetchMonadError
import com.twitter.conversions.time._
import com.twitter.util.{Await, Future, FuturePool}
import fetch._

final case class FetchedObjectCache(underlyingCache: SimpleCache) extends DataSourceCache {
  override def get[A](k: DataSourceIdentity): Option[A] = {
    // Failures & timeouts from the underlying cache return None, causing a fetch from the datasource.
    val result = Either.catchNonFatal {
      val cacheGetFuture = underlyingCache.get[A](CacheKey(k.toString))
      Await.result(cacheGetFuture, timeout = 5.seconds)
    }
    result.getOrElse(None)
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
