package com.absinthe.libchecker.utils.elf.source

import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

interface DataSource : Closeable {
  @Throws(IOException::class)
  fun read(buffer: ByteBuffer, offset: Long)
  
  @Throws(IOException::class)  
  fun read(offset: Long, buf: ByteArray, length: Int)
  
  @Throws(IOException::class)
  fun readWord(offset: Long): Long
  
  @Throws(IOException::class)
  fun readHalf(offset: Long): Int
  
  @Throws(IOException::class)
  fun readLong(offset: Long): Long
  
  @Throws(IOException::class)
  fun readInt(offset: Long): Int
  
  @Throws(IOException::class)
  fun readByte(offset: Long): Short
  
  @Throws(IOException::class)
  fun readString(offset: Long): String
  
  @Throws(IOException::class)
  fun position(offset: Long)
  
  val order: ByteOrder
  
  fun setOrder(order: ByteOrder)
}