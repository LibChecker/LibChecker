package com.absinthe.libchecker.utils.elf.source;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteBufferDataSource implements DataSource {
    private final ByteBuffer buffer;
    private ByteOrder order = ByteOrder.LITTLE_ENDIAN;

    public ByteBufferDataSource(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void read(ByteBuffer dst, long offset) throws IOException {
        buffer.position((int) offset);
        dst.put(buffer);
        dst.flip();
    }

    @Override
    public void read(long offset, byte[] buf, int length) throws IOException {
        buffer.position((int) offset);
        buffer.get(buf, 0, length);
    }

    @Override
    public long readWord(long offset) throws IOException {
        return buffer.getInt((int) offset) & 0xFFFFFFFFL;
    }

    @Override
    public int readHalf(long offset) throws IOException {
        return buffer.getShort((int) offset) & 0xFFFF;
    }

    @Override
    public long readLong(long offset) throws IOException {
        return buffer.getLong((int) offset);
    }

    @Override
    public int readInt(long offset) throws IOException {
        return buffer.getInt((int) offset);
    }

    @Override
    public short readByte(long offset) throws IOException {
        return (short) (buffer.get((int) offset) & 0xFF);
    }

    @Override
    public String readString(long offset) throws IOException {
        StringBuilder sb = new StringBuilder();
        short c;
        while ((c = readByte(offset++)) != 0) {
            sb.append((char) c);
        }
        return sb.toString();
    }

    @Override
    public void position(long offset) throws IOException {
        buffer.position((int) offset);
    }

    @Override
    public ByteOrder getOrder() {
        return order;
    }

    @Override
    public void setOrder(ByteOrder order) {
        this.order = order;
        this.buffer.order(order);
    }

    @Override
    public void close() throws IOException {
        // Nothing to do
    }
}

