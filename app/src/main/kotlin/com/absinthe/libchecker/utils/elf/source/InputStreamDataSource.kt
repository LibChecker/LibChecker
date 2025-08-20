package com.absinthe.libchecker.utils.elf.source

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class InputStreamDataSource @Throws(IOException::class) constructor(
    private val inputStream: InputStream
) : DataSource {
    private var buffer: ByteBuffer
    private var _order: ByteOrder = ByteOrder.LITTLE_ENDIAN
    private var allDataRead = false

    companion object {
        private const val INITIAL_BUFFER_SIZE = 4096
    }

    init {
        readInitialBuffer()
    }

    @Throws(IOException::class)
    private fun readInitialBuffer() {
        val initialBytes = ByteArray(INITIAL_BUFFER_SIZE)
        var totalBytesRead = 0
        var bytesRead: Int
        
        while (totalBytesRead < INITIAL_BUFFER_SIZE) {
            bytesRead = inputStream.read(initialBytes, totalBytesRead, INITIAL_BUFFER_SIZE - totalBytesRead)
            if (bytesRead == -1) break
            totalBytesRead += bytesRead
        }

        buffer = if (totalBytesRead > 0) {
            if (totalBytesRead < INITIAL_BUFFER_SIZE) {
                val exactBytes = ByteArray(totalBytesRead)
                System.arraycopy(initialBytes, 0, exactBytes, 0, totalBytesRead)
                allDataRead = true
                ByteBuffer.wrap(exactBytes)
            } else {
                ByteBuffer.wrap(initialBytes)
            }
        } else {
            allDataRead = true
            ByteBuffer.wrap(ByteArray(0))
        }
        buffer.order(_order)
    }

    @Throws(IOException::class)
    private fun ensureAvailable(offset: Long, length: Int) {
        if (allDataRead) {
            if (offset + length > buffer.capacity()) {
                throw IOException("Attempt to read past end of stream.")
            }
            return
        }
        
        if (offset + length > buffer.capacity()) {
            val baos = ByteArrayOutputStream()
            baos.write(buffer.array())
            val chunk = ByteArray(4096)
            var n: Int
            while (inputStream.read(chunk).also { n = it } != -1) {
                baos.write(chunk, 0, n)
            }
            buffer = ByteBuffer.wrap(baos.toByteArray())
            buffer.order(_order)
            allDataRead = true
            
            if (offset + length > buffer.capacity()) {
                throw IOException("Attempt to read past end of stream after reading all data.")
            }
        }
    }

    @Throws(IOException::class)
    override fun read(dst: ByteBuffer, offset: Long) {
        ensureAvailable(offset, dst.remaining())
        buffer.position(offset.toInt())
        val slice = buffer.slice()
        slice.limit(dst.remaining())
        dst.put(slice)
        dst.flip()
    }

    @Throws(IOException::class)
    override fun read(offset: Long, buf: ByteArray, length: Int) {
        ensureAvailable(offset, length)
        buffer.position(offset.toInt())
        buffer.get(buf, 0, length)
    }

    @Throws(IOException::class)
    override fun readWord(offset: Long): Long {
        ensureAvailable(offset, 4)
        return buffer.getInt(offset.toInt()) and 0xFFFFFFFFL
    }

    @Throws(IOException::class)
    override fun readHalf(offset: Long): Int {
        ensureAvailable(offset, 2)
        return buffer.getShort(offset.toInt()) and 0xFFFF
    }

    @Throws(IOException::class)
    override fun readLong(offset: Long): Long {
        ensureAvailable(offset, 8)
        return buffer.getLong(offset.toInt())
    }

    @Throws(IOException::class)
    override fun readInt(offset: Long): Int {
        ensureAvailable(offset, 4)
        return buffer.getInt(offset.toInt())
    }

    @Throws(IOException::class)
    override fun readByte(offset: Long): Short {
        ensureAvailable(offset, 1)
        return (buffer.get(offset.toInt()) and 0xFF.toByte()).toShort()
    }

    @Throws(IOException::class)
    override fun readString(offset: Long): String {
        val sb = StringBuilder()
        var currentOffset = offset
        
        while (true) {
            ensureAvailable(currentOffset, 1)
            val c = readByte(currentOffset++)
            if (c == 0.toShort()) {
                break
            }
            sb.append(c.toInt().toChar())
        }
        return sb.toString()
    }

    @Throws(IOException::class)
    override fun position(offset: Long) {
        ensureAvailable(offset, 1)
        buffer.position(offset.toInt())
    }

    override val order: ByteOrder
        get() = _order

    override fun setOrder(order: ByteOrder) {
        this._order = order
        this.buffer.order(order)
    }

    @Throws(IOException::class)
    override fun close() {
        inputStream.close()
    }
}