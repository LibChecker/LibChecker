package com.absinthe.libchecker.utils.elf.source

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ByteBufferDataSource(private val buffer: ByteBuffer) : DataSource {
    private var _order: ByteOrder = ByteOrder.LITTLE_ENDIAN

    @Throws(IOException::class)
    override fun read(dst: ByteBuffer, offset: Long) {
        buffer.position(offset.toInt())
        dst.put(buffer)
        dst.flip()
    }

    @Throws(IOException::class)
    override fun read(offset: Long, buf: ByteArray, length: Int) {
        buffer.position(offset.toInt())
        buffer.get(buf, 0, length)
    }

    @Throws(IOException::class)
    override fun readWord(offset: Long): Long {
        return buffer.getInt(offset.toInt()) and 0xFFFFFFFFL
    }

    @Throws(IOException::class)
    override fun readHalf(offset: Long): Int {
        return buffer.getShort(offset.toInt()) and 0xFFFF
    }

    @Throws(IOException::class)
    override fun readLong(offset: Long): Long {
        return buffer.getLong(offset.toInt())
    }

    @Throws(IOException::class)
    override fun readInt(offset: Long): Int {
        return buffer.getInt(offset.toInt())
    }

    @Throws(IOException::class)
    override fun readByte(offset: Long): Short {
        return (buffer.get(offset.toInt()) and 0xFF.toByte()).toShort()
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
        // Nothing to do
    }
}