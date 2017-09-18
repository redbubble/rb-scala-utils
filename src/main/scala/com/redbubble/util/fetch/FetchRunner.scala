package com.redbubble.util.fetch

import cats.syntax.either._
import com.redbubble.util.cache.{CacheKey, SimpleCache}
import com.redbubble.util.fetch.TwitterFutureFetchMonadError.twitterFutureFetchMonadError
import com.twitter.conversions.time._
import com.twitter.util.{Await, Future, FuturePool, TimeoutException}
import fetch._

final case class FetchedObjectCache(underlyingCache: SimpleCache) extends DataSourceCache {
  /**
    * We rescue failures & timeouts from the underlying cache by returning None, which will cause a fetch from the
    * datasource.
    */
  override def get[A](k: DataSourceIdentity): Option[A] = {
    val cacheGet = underlyingCache.get[A](CacheKey(k.toString)).rescue {
      case _ => Future.value(None)
    }
    Either.catchOnly[TimeoutException] {
      Await.result(cacheGet, timeout = 5.seconds)
    }.getOrElse(None)
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
