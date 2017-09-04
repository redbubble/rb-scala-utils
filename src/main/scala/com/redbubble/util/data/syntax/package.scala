package com.redbubble.util.data

package object syntax {

  implicit final class RandomSequence[T](s: Seq[T]) {
    def randomElement(): Seq[T] = randomSlice(1)

    def randomSlice(size: Int): Seq[T] = SeqOps.randomTake(s, size)
  }

}
