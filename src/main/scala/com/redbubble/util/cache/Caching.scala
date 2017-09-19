package com.redbubble.util.cache

import java.util.concurrent.{Executor, TimeUnit}

import com.redbubble.util.async.syntax._
import com.twitter.util.{Duration, Future}

import scala.concurrent.ExecutionContext.fromExecutor
import scala.concurrent.duration.{Duration => ScalaDuration}
import scalacache.serialization.Codec
import scalacache.{Flags, ScalaCache}

/**
  * Common implementations of the underlying caching mechanics, transforming of isomorphic types, etc.
  */
private[cache] object Caching {
  private val flags = Flags(readsEnabled = true, writesEnabled = true)

  /**
    * Cache the result of executing `f` using the given `key`.
    *
    * @param cache The underlying cache.
    * @param ttl   The TTL of the item to be cached.
    * @param key   The key to use to cache the value.
    * @param codec The codec to use to serialise/encide the value into the cache.
    * @param f     The function to run, whose result will be cached.
    * @param ex    The executor to use to run any asynchronous operations.
    * @tparam V    The type of the value to be cached.
    * @tparam Repr The type of the serialised/encoded form of the value, e.g. `InMemoryRepr` or `Array[Byte]`.
    * @return A Twitter `Future` with the result of `f`.
    */
  def caching[V, Repr](cache: ScalaCache[Repr], ttl: Duration, key: CacheKey, codec: Codec[V, Repr])(f: => Future[V])(implicit ex: Executor): Future[V] = {
    Caching.caching(cache, ttl, key, codec)(f)(ex)

    val ec = toEc(ex)
    val result = scalacache.cachingWithTTL(key)(toScalaTtl(ttl))(f.asScala)(cache, flags, ec, codec)
    result.asTwitter(ec)
  }

  /**
    * Manually put a value into the cache. This function is side-effecting. Prefer `caching` instead of this function.
    *
    * @param cache The underlying cache.
    * @param ttl   The TTL of the item to be cached.
    * @param key   The key to use to cache the value.
    * @param codec The codec to use to serialise/encide the value into the cache.
    * @param value The value to cache.
    * @param ex    The executor to use to run any asynchronous operations.
    * @tparam V    The type of the value to be cached.
    * @tparam Repr The type of the serialised/encoded form of the value, e.g. `InMemoryRepr` or `Array[Byte]`.
    * @return A Twitter `Future` containing no result (you can use this to know when the value has been cached).
    */
  def put[V, Repr](cache: ScalaCache[Repr], ttl: Duration, key: CacheKey, codec: Codec[V, Repr], value: V)(implicit ex: Executor): Future[Unit] = {
    val ec = toEc(ex)
    val result = scalacache.put(key)(value, Some(toScalaTtl(ttl)))(cache, flags, codec)
    result.asTwitter(ec)
  }

  /**
    * Manually get a value from the cache. Prefer `caching` instead of this function.
    *
    * @param cache The underlying cache.
    * @param key   The key to use to cache the value.
    * @param codec The codec to use to serialise/encide the value into the cache.
    * @param ex    The executor to use to run any asynchronous operations.
    * @tparam V    The type of the value to be cached.
    * @tparam Repr The type of the serialised/encoded form of the value, e.g. `InMemoryRepr` or `Array[Byte]`.
    * @return A Twitter `Future` the value from the cache.
    */
  def get[V, Repr](cache: ScalaCache[Repr], key: CacheKey, codec: Codec[V, Repr])(implicit ex: Executor): Future[Option[V]] = {
    val ec = toEc(ex)
    val result = scalacache.get(key)(cache, flags, codec)
    result.asTwitter(ec)
  }

  /**
    * Flush (clear) the cache of all entries.
    */
  def flush[Repr](cache: ScalaCache[Repr])(implicit ex: Executor): Future[Unit] =
    cache.cache.removeAll().asTwitter(toEc(ex))

  private def toEc(ex: Executor) = fromExecutor(ex)

  private def toScalaTtl(ttl: Duration): ScalaDuration = ScalaDuration(ttl.inNanoseconds, TimeUnit.NANOSECONDS)
}
