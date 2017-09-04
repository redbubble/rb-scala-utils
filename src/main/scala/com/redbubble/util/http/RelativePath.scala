package com.redbubble.util.http

sealed trait UriPath {
  def path: String
}

final case class RelativePath(path: String) extends UriPath

final case class AbsolutePath(path: String) extends UriPath

object RelativePath {
  val emptyPath: RelativePath = RelativePath("")
  val emptyAbsolutePath: AbsolutePath = AbsolutePath("")
}
