package com.absinthe.libchecker.utils.elf.source

import java.io.EOFException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class FileChannelDataSource(private val channel: FileChannel) : DataSource {
    private var _order: ByteOrder = ByteOrder.LITTLE_ENDIAN

    @Throws(IOException::class)
    override fun read(buffer: ByteBuffer, offset: Long) {
        buffer.rewind()
        val bytesRead = channel.read(buffer, offset)
        if (bytesRead != buffer.limit()) {
            throw EOFException()
        }
        buffer.flip()
    }

    @Throws(IOException::class)
    override fun read(offset: Long, buf: ByteArray, length: Int) {
        val b = ByteBuffer.wrap(buf, 0, length)
        read(b, offset)
    }

    @Throws(IOException::class)
    override fun readWord(offset: Long): Long {
        val b = ByteBuffer.allocate(4)
        b.order(_order)
        read(b, offset)
        return b.int and 0xFFFFFFFFL
    }

    @Throws(IOException::class)
    override fun readHalf(offset: Long): Int {
        val b = ByteBuffer.allocate(2)
        b.order(_order)
        read(b, offset)
        return b.short and 0xFFFF
    }

    @Throws(IOException::class)
    override fun readLong(offset: Long): Long {
        val b = ByteBuffer.allocate(8)
        b.order(_order)
        read(b, offset)
        return b.long
    }

    @Throws(IOException::class)
    override fun readInt(offset: Long): Int {
        val b = ByteBuffer.allocate(4)
        b.order(_order)
        read(b, offset)
        return b.int
    }

    @Throws(IOException::class)
    override fun readByte(offset: Long): Short {
        val b = ByteBuffer.allocate(1)
        read(b, offset)
        return (b.get() and 0xFF.toByte()).toShort()
    }

    @Throws(IOException::class)
    override fun readString(offset: Long): String {
        val sb = StringBuilder()
        var currentOffset = offset
        var c: Short
        while (readByte(currentOffset++).also { c = it } != 0.toShort()) {
            sb.append(c.toInt().toChar())
        }
        return sb.toString()
    }

    @Throws(IOException::class)
    override fun position(offset: Long) {
        channel.position(offset)
    }

    override val order: ByteOrder
        get() = _order

    override fun setOrder(order: ByteOrder) {
        this._order = order
    }

    @Throws(IOException::class)
    override fun close() {
        channel.close()
    }
}