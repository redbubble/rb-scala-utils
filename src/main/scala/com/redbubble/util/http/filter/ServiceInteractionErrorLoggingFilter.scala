package com.redbubble.util.http.filter

import java.net.URL

import com.redbubble.util.http.Errors.downstreamError
import com.redbubble.util.http.{DownstreamError, ServiceInteraction}
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future

object ServiceInteractionErrorLoggingFilter {
  def interactionFilter(baseUrl: URL): HttpFilter = new ServiceInteractionErrorLoggingFilter(baseUrl)
}

/**
  * A filter that wraps errors from services with details of the interaction, e.g. the downstream URL.
  *
  * @param baseUrl The base URL to the service this filter is mixed in to.
  */
final class ServiceInteractionErrorLoggingFilter(baseUrl: URL) extends HttpFilter {
  def apply(request: Request, service: Service[Request, Response]): Future[Response] =
    service(request).rescue {
      case de: DownstreamError => Future.exception(de)
      case t: Throwable =>
        val interaction = ServiceInteraction.interaction(baseUrl, Seq.empty, None, None)
        Future.exception(downstreamError(t, interaction))
    }
}
