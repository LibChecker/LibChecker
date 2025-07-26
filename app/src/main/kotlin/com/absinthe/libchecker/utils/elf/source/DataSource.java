package com.absinthe.libchecker.utils.elf.source;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public interface DataSource extends Closeable {
    void read(ByteBuffer buffer, long offset) throws IOException;
    void read(long offset, byte[] buf, int length) throws IOException;
    long readWord(long offset) throws IOException;
    int readHalf(long offset) throws IOException;
    long readLong(long offset) throws IOException;
    int readInt(long offset) throws IOException;
    short readByte(long offset) throws IOException;
    String readString(long offset) throws IOException;
    void position(long offset) throws IOException;
    ByteOrder getOrder();
    void setOrder(ByteOrder order);
}

