package com.absinthe.libchecker.utils.elf.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class InputStreamDataSource implements DataSource {
    private final InputStream inputStream;
    private ByteBuffer buffer;
    private ByteOrder order = ByteOrder.LITTLE_ENDIAN;
    private boolean allDataRead = false;
    private int validLength = 0;
    private static final int INITIAL_BUFFER_SIZE = 4096;

    public InputStreamDataSource(InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
        readInitialBuffer();
    }

    private void readInitialBuffer() throws IOException {
        byte[] initialBytes = new byte[INITIAL_BUFFER_SIZE];
        int bytesRead;
        while (validLength < INITIAL_BUFFER_SIZE
                && (bytesRead = inputStream.read(initialBytes, validLength, INITIAL_BUFFER_SIZE - validLength)) != -1) {
            if (bytesRead == 0) {
                int value = inputStream.read();
                if (value == -1) {
                    break;
                }
                initialBytes[validLength++] = (byte) value;
                continue;
            }
            validLength += bytesRead;
        }

        if (validLength < INITIAL_BUFFER_SIZE) {
            allDataRead = true;
        }
        this.buffer = ByteBuffer.wrap(initialBytes);
        this.buffer.order(order);
    }

    private void ensureAvailable(long offset, int length) throws IOException {
        if (offset < 0 || length < 0 || offset > Integer.MAX_VALUE - length) {
            throw new IOException("Requested stream range is invalid.");
        }

        int requiredLength = (int) offset + length;
        if (requiredLength <= validLength) {
            return;
        }
        if (allDataRead) {
            throw new IOException("Attempt to read past end of stream.");
        }

        while (requiredLength > validLength && !allDataRead) {
            if (validLength == buffer.capacity()) {
                int nextCapacity;
                if (buffer.capacity() > Integer.MAX_VALUE / 2) {
                    nextCapacity = requiredLength;
                } else {
                    nextCapacity = Math.min(requiredLength, buffer.capacity() * 2);
                }
                byte[] expanded = Arrays.copyOf(buffer.array(), nextCapacity);
                this.buffer = ByteBuffer.wrap(expanded);
                this.buffer.order(order);
            }

            int bytesRead = inputStream.read(
                    buffer.array(),
                    validLength,
                    buffer.capacity() - validLength
            );
            if (bytesRead == -1) {
                allDataRead = true;
                continue;
            }
            if (bytesRead > 0) {
                validLength += bytesRead;
                continue;
            }

            int value = inputStream.read();
            if (value == -1) {
                allDataRead = true;
            } else {
                buffer.array()[validLength++] = (byte) value;
            }
        }

        if (requiredLength > validLength) {
            throw new IOException("Attempt to read past end of stream.");
        }
    }

    @Override
    public void read(ByteBuffer dst, long offset) throws IOException {
        ensureAvailable(offset, dst.remaining());
        buffer.position((int) offset);
        ByteBuffer slice = buffer.slice();
        slice.limit(dst.remaining());
        dst.put(slice);
        dst.flip();
    }

    @Override
    public void read(long offset, byte[] buf, int length) throws IOException {
        ensureAvailable(offset, length);
        buffer.position((int) offset);
        buffer.get(buf, 0, length);
    }

    @Override
    public long readWord(long offset) throws IOException {
        ensureAvailable(offset, 4);
        return buffer.getInt((int) offset) & 0xFFFFFFFFL;
    }

    @Override
    public int readHalf(long offset) throws IOException {
        ensureAvailable(offset, 2);
        return buffer.getShort((int) offset) & 0xFFFF;
    }

    @Override
    public long readLong(long offset) throws IOException {
        ensureAvailable(offset, 8);
        return buffer.getLong((int) offset);
    }

    @Override
    public int readInt(long offset) throws IOException {
        ensureAvailable(offset, 4);
        return buffer.getInt((int) offset);
    }

    @Override
    public short readByte(long offset) throws IOException {
        ensureAvailable(offset, 1);
        return (short) (buffer.get((int) offset) & 0xFF);
    }

    @Override
    public String readString(long offset) throws IOException {
        StringBuilder sb = new StringBuilder();
        short c;
        long currentOffset = offset;
        while (true) {
            ensureAvailable(currentOffset, 1);
            c = readByte(currentOffset++);
            if (c == 0) {
                break;
            }
            sb.append((char) c);
        }
        return sb.toString();
    }

    @Override
    public void position(long offset) throws IOException {
        ensureAvailable(offset, 1);
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
        inputStream.close();
    }
}
