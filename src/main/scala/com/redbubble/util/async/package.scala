package com.redbubble.util

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.newSingleThreadExecutor

import com.redbubble.util.async.AsyncOps.shutdownExecutorService
import com.twitter.util.FuturePool

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.fromExecutor

/**
  * Common execution services, backed by the same underlying thread pool, for various usages (Java, Scala & Finagle).
  */
package object async {
  lazy val singleThreadedExecutor: ExecutorService = newSingleThreadExecutor
  lazy val singleThreadedExecutionContext: ExecutionContext = fromExecutor(singleThreadedExecutor)
  lazy val singleThreadedFuturePool: FuturePool = FuturePool.interruptible(singleThreadedExecutor)

  sys.addShutdownHook(shutdownExecutorService(singleThreadedExecutor))
}
