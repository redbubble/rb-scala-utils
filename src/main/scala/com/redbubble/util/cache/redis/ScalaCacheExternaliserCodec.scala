package com.redbubble.util.cache.redis

import com.twitter.chill.Externalizer

import scalacache.serialization.{Codec, GZippingJavaAnyBinaryCodec}

// TODO Can we memoise/cache any of these little objects we're creating?
private[redis] final class ScalaCacheExternaliserCodec[V] extends Codec[V, Array[Byte]] {
  private val underlyingCodec = GZippingJavaAnyBinaryCodec.default[Externalizer[V]]

  def serialize(value: V): Array[Byte] = {
    val externalised: Externalizer[V] = Externalizer(value)
    underlyingCodec.serialize(externalised)
  }

  def deserialize(data: Array[Byte]): V = {
    val externalised: Externalizer[V] = underlyingCodec.deserialize(data)
    externalised.get
  }
}
