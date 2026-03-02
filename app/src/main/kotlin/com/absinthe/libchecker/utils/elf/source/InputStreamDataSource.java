package com.absinthe.libchecker.utils.elf.source;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class InputStreamDataSource implements DataSource {
    private final InputStream inputStream;
    private ByteBuffer buffer;
    private ByteOrder order = ByteOrder.LITTLE_ENDIAN;
    private boolean allDataRead = false;
    private static final int INITIAL_BUFFER_SIZE = 4096;

    public InputStreamDataSource(InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
        readInitialBuffer();
    }

    private void readInitialBuffer() throws IOException {
        byte[] initialBytes = new byte[INITIAL_BUFFER_SIZE];
        int totalBytesRead = 0;
        int bytesRead;
        while (totalBytesRead < INITIAL_BUFFER_SIZE && (bytesRead = inputStream.read(initialBytes, totalBytesRead, INITIAL_BUFFER_SIZE - totalBytesRead)) != -1) {
            totalBytesRead += bytesRead;
        }

        if (totalBytesRead > 0) {
            if (totalBytesRead < INITIAL_BUFFER_SIZE) {
                byte[] exactBytes = new byte[totalBytesRead];
                System.arraycopy(initialBytes, 0, exactBytes, 0, totalBytesRead);
                this.buffer = ByteBuffer.wrap(exactBytes);
                allDataRead = true;
            } else {
                this.buffer = ByteBuffer.wrap(initialBytes);
            }
        } else {
            this.buffer = ByteBuffer.wrap(new byte[0]);
            allDataRead = true;
        }
        this.buffer.order(order);
    }

    private void ensureAvailable(long offset, int length) throws IOException {
        if (allDataRead) {
            if (offset + length > buffer.capacity()) {
                throw new IOException("Attempt to read past end of stream.");
            }
            return;
        }
        if (offset + length > buffer.capacity()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(buffer.array());
            byte[] chunk = new byte[4096];
            int n;
            while ((n = inputStream.read(chunk)) != -1) {
                baos.write(chunk, 0, n);
            }
            this.buffer = ByteBuffer.wrap(baos.toByteArray());
            this.buffer.order(order);
            allDataRead = true;
            if (offset + length > buffer.capacity()) {
                 throw new IOException("Attempt to read past end of stream after reading all data.");
            }
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

