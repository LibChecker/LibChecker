package com.absinthe.libchecker.utils.elf.source;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class FileChannelDataSource implements DataSource {
    private final FileChannel channel;
    private ByteOrder order = ByteOrder.LITTLE_ENDIAN;

    public FileChannelDataSource(FileChannel channel) {
        this.channel = channel;
    }

    @Override
    public void read(ByteBuffer buffer, long offset) throws IOException {
        buffer.rewind();
        int bytesRead = channel.read(buffer, offset);
        if (bytesRead != buffer.limit()) {
            throw new EOFException();
        }
        buffer.flip();
    }

    @Override
    public void read(long offset, byte[] buf, int length) throws IOException {
        ByteBuffer b = ByteBuffer.wrap(buf, 0, length);
        read(b, offset);
    }

    @Override
    public long readWord(long offset) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(order);
        read(b, offset);
        return b.getInt() & 0xFFFFFFFFL;
    }

    @Override
    public int readHalf(long offset) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(2);
        b.order(order);
        read(b, offset);
        return b.getShort() & 0xFFFF;
    }

    @Override
    public long readLong(long offset) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(8);
        b.order(order);
        read(b, offset);
        return b.getLong();
    }

    @Override
    public int readInt(long offset) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(order);
        read(b, offset);
        return b.getInt();
    }

    @Override
    public short readByte(long offset) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(1);
        read(b, offset);
        return (short) (b.get() & 0xFF);
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
        channel.position(offset);
    }

    @Override
    public ByteOrder getOrder() {
        return order;
    }

    @Override
    public void setOrder(ByteOrder order) {
        this.order = order;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}

