package com.redbubble.util.spec.http

import java.net.URI

import com.redbubble.util.http.{AbsolutePath, QueryParam}
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, RequestBuilder, Response}
import com.twitter.io.Buf
import com.twitter.util.Await

trait HttpSpecHelpers {
  final def GET(path: String, params: Seq[QueryParam] = Seq.empty): Request = GET(AbsolutePath(path), params)

  final def GET(path: AbsolutePath, params: Seq[QueryParam]): Request =
    RequestBuilder().url(uriForPathAndParams(path, params).toURL).buildGet()

  final def POST(path: String, params: Seq[QueryParam] = Seq.empty, content: Buf = Buf.Empty): Request =
    POST(AbsolutePath(path), params, content)

  final def POST(path: AbsolutePath, params: Seq[QueryParam], content: Buf): Request =
    RequestBuilder().url(uriForPathAndParams(path, params).toURL).buildPost(content)

  final def awaitResponse(service: Service[Request, Response]): Request => Response = (req) => Await.result(service(req))

  //noinspection ScalaStyle
  private def uriForPathAndParams(path: AbsolutePath, params: Seq[QueryParam]): URI =
    new URI("https", "localhost", path.path, paramsAsQueryString(params), null)

  private def paramsAsQueryString(params: Seq[QueryParam]): String = params.map(p => s"${p._1}=${p._2}").mkString("&")
}
