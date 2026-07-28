package com.absinthe.libchecker.utils.apk

import java.io.Closeable
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

internal interface SeekableInput : Closeable {
  val size: Long

  var position: Long

  fun read(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size): Int

  fun readFully(buffer: ByteArray) {
    var offset = 0
    while (offset < buffer.size) {
      val read = read(buffer, offset, buffer.size - offset)
      if (read <= 0) {
        throw EOFException("Unexpected end of seekable input")
      }
      offset += read
    }
  }
}

internal class FileSeekableInput(file: File) : SeekableInput {
  private val inputStream = FileInputStream(file)
  private val channel: FileChannel = inputStream.channel

  override val size: Long
    get() = channel.size()

  override var position: Long
    get() = channel.position()
    set(value) {
      channel.position(value)
    }

  override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
    return channel.read(ByteBuffer.wrap(buffer, offset, length))
  }

  override fun close() {
    inputStream.close()
  }
}
