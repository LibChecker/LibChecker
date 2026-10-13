package com.absinthe.libchecker.utils.elf.source;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class InputStreamDataSource implements DataSource {
    private final InputStream inputStream;
    private final Runnable checkCancellation;
    private ByteBuffer buffer;
    private final ByteBuffer primitiveBuffer = ByteBuffer.allocate(8);
    private File overflowFile;
    private RandomAccessFile overflowAccessFile;
    private FileChannel overflowChannel;
    private byte[] transferBuffer;
    private ByteOrder order = ByteOrder.LITTLE_ENDIAN;
    private boolean allDataRead = false;
    private int validLength = 0;
    private static final int INITIAL_BUFFER_SIZE = 4096;
    static final int MAX_IN_MEMORY_BYTES = 1024 * 1024;

    int bufferedInMemoryBytes() {
        return buffer != null ? buffer.capacity() : 0;
    }

    public InputStreamDataSource(InputStream inputStream) throws IOException {
        this(inputStream, () -> {});
    }

    public InputStreamDataSource(InputStream inputStream, Runnable checkCancellation) throws IOException {
        this.checkCancellation = checkCancellation;
        this.inputStream = inputStream;
        try {
            readInitialBuffer();
        } catch (IOException | RuntimeException | Error error) {
            try { inputStream.close(); } catch (IOException closeError) { error.addSuppressed(closeError); }
            throw error;
        }
    }

    private void readInitialBuffer() throws IOException {
        checkCancellation.run();
        byte[] initialBytes = new byte[INITIAL_BUFFER_SIZE];
        int bytesRead;
        while (validLength < INITIAL_BUFFER_SIZE
                && (bytesRead = inputStream.read(initialBytes, validLength, INITIAL_BUFFER_SIZE - validLength)) != -1) {
            checkCancellation.run();
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
        checkCancellation.run();
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
            checkCancellation.run();
            if (overflowChannel != null) {
                readNextChunkToOverflow();
                continue;
            }
            if (validLength == buffer.capacity()) {
                if (validLength >= MAX_IN_MEMORY_BYTES) {
                    spillToOverflowFile();
                    readNextChunkToOverflow();
                    continue;
                }
                int nextCapacity;
                if (buffer.capacity() > Integer.MAX_VALUE / 2) {
                    nextCapacity = Math.min(MAX_IN_MEMORY_BYTES, requiredLength);
                } else {
                    nextCapacity = Math.min(MAX_IN_MEMORY_BYTES, Math.min(requiredLength, buffer.capacity() * 2));
                }
                byte[] expanded = Arrays.copyOf(buffer.array(), nextCapacity);
                this.buffer = ByteBuffer.wrap(expanded);
                this.buffer.order(order);
            }

            int bytesRead = inputStream.read(
                    buffer.array(),
                    validLength,
                    Math.min(8192, buffer.capacity() - validLength)
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

    private void spillToOverflowFile() throws IOException {
        File tempFile = File.createTempFile("libchecker-elf-", ".bin");
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(tempFile, "rw");
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
            FileChannel channel = raf.getChannel();
            ByteBuffer initial = ByteBuffer.wrap(buffer.array(), 0, validLength);
            while (initial.hasRemaining()) {
                checkCancellation.run();
                channel.write(initial);
            }
            this.overflowFile = tempFile;
            this.overflowAccessFile = raf;
            this.overflowChannel = channel;
            this.transferBuffer = new byte[INITIAL_BUFFER_SIZE];
            this.buffer = null;
        } catch (IOException | RuntimeException | Error error) {
            if (raf != null) {
                try { raf.close(); } catch (IOException closeError) { error.addSuppressed(closeError); }
            }
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
            throw error;
        }
    }

    private void readNextChunkToOverflow() throws IOException {
        int bytesRead = inputStream.read(transferBuffer, 0, transferBuffer.length);
        if (bytesRead == -1) {
            allDataRead = true;
            return;
        }
        if (bytesRead == 0) {
            int value = inputStream.read();
            if (value == -1) {
                allDataRead = true;
                return;
            }
            transferBuffer[0] = (byte) value;
            bytesRead = 1;
        }
        ByteBuffer chunk = ByteBuffer.wrap(transferBuffer, 0, bytesRead);
        long writeOffset = validLength;
        while (chunk.hasRemaining()) {
            writeOffset += overflowChannel.write(chunk, writeOffset);
        }
        validLength += bytesRead;
    }

    private void readFromOverflow(ByteBuffer dst, long offset) throws IOException {
        long pos = offset;
        while (dst.hasRemaining()) {
            int n = overflowChannel.read(dst, pos);
            if (n < 0) {
                throw new IOException("Attempt to read past end of stream.");
            }
            pos += n;
        }
        dst.flip();
    }

    private ByteBuffer readPrimitiveBytes(long offset, int size) throws IOException {
        ensureAvailable(offset, size);
        if (buffer != null) {
            buffer.position((int) offset);
            return buffer;
        }
        primitiveBuffer.clear();
        primitiveBuffer.limit(size);
        primitiveBuffer.order(order);
        readFromOverflow(primitiveBuffer, offset);
        return primitiveBuffer;
    }

    @Override
    public void read(ByteBuffer dst, long offset) throws IOException {
        int remaining = dst.remaining();
        ensureAvailable(offset, remaining);
        if (buffer != null) {
            buffer.position((int) offset);
            ByteBuffer slice = buffer.slice();
            slice.limit(remaining);
            dst.put(slice);
            dst.flip();
        } else {
            readFromOverflow(dst, offset);
        }
    }

    @Override
    public void read(long offset, byte[] buf, int length) throws IOException {
        ensureAvailable(offset, length);
        if (buffer != null) {
            buffer.position((int) offset);
            buffer.get(buf, 0, length);
        } else {
            readFromOverflow(ByteBuffer.wrap(buf, 0, length), offset);
        }
    }

    @Override
    public long readWord(long offset) throws IOException {
        return readPrimitiveBytes(offset, 4).getInt() & 0xFFFFFFFFL;
    }

    @Override
    public int readHalf(long offset) throws IOException {
        return readPrimitiveBytes(offset, 2).getShort() & 0xFFFF;
    }

    @Override
    public long readLong(long offset) throws IOException {
        return readPrimitiveBytes(offset, 8).getLong();
    }

    @Override
    public int readInt(long offset) throws IOException {
        return readPrimitiveBytes(offset, 4).getInt();
    }

    @Override
    public short readByte(long offset) throws IOException {
        return (short) (readPrimitiveBytes(offset, 1).get() & 0xFF);
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
        if (buffer != null) {
            buffer.position((int) offset);
        } else {
            overflowChannel.position(offset);
        }
    }

    @Override
    public ByteOrder getOrder() {
        return order;
    }

    @Override
    public void setOrder(ByteOrder order) {
        this.order = order;
        if (this.buffer != null) {
            this.buffer.order(order);
        }
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        try {
            inputStream.close();
        } catch (IOException e) {
            failure = e;
        }
        if (overflowAccessFile != null) {
            try {
                overflowAccessFile.close();
            } catch (IOException e) {
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
        }
        if (overflowFile != null) {
            //noinspection ResultOfMethodCallIgnored
            overflowFile.delete();
        }
        if (failure != null) {
            throw failure;
        }
    }
}
