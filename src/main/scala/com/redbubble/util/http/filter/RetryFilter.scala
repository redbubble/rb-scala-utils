package com.redbubble.util.http.filter

import com.twitter.finagle.Service
import com.twitter.finagle.param.HighResTimer
import com.twitter.finagle.service.{RetryFilter => TwitterRetryFilter}
import com.twitter.util.{Duration, Try}

object RetryFilter {
  type RetryCondition[Req, Resp] = PartialFunction[(Req, Try[Resp]), Boolean]

  private val timer = HighResTimer.Default

  /**
    * Creates a filter that retries requests to an underlying `service`.
    *
    * Can be used in client code as follows:
    * {{{
    * // retry once a second, 15 times
    * private val retries = Backoff.const(1.second).take(15)
    *
    * val retryingApiClient = {
    *   val c = new IosHttpClient(new URL(...)) {
    *       override protected def clientTransform(client: Client) = connectionPoolAndTimeoutConfig(client)
    *
    *       override protected def serviceTransform[Req, Resp](service: Service[Req, Resp]) = RetryFilter.retry(service, retries, retryCondition)
    *     }
    *     client(c, env.knowledgeGraphApiUrl.getHost)(clientMetrics)
    *   }
    *
    * private def retryCondition[Req, Resp]: RetryCondition[Req, Resp] = {
    *   case (_, Return(rep: Response)) => rep.status == Accepted
    * }
    *
    * val response = retryingApiClient.get(RelativePath("path-to-retry-on"))
    * }}}
    *
    *
    * @param service        The service to retry.
    * @param retries        A stream of durations, where each duration represents a retry attempt, e.g. a stream containing
    *                       three durations would retry three times, once for each duration.
    * @param retryCondition When to retry a request.
    */
  final def retry[Req, Resp](service: Service[Req, Resp],
      retries: Stream[Duration], retryCondition: RetryCondition[Req, Resp]): Service[Req, Resp] = {
    val retryFilter = TwitterRetryFilter(retries)(retryCondition)(timer)
    retryFilter.andThen(service)
  }
}
