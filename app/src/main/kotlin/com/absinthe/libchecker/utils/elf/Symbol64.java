package com.absinthe.libchecker.utils.elf;

import java.io.IOException;

public class Symbol64 extends Elf.Symbol {
  public Symbol64(ElfParser parser, long offset) throws IOException {
    name = parser.readWord(offset);
    info = (byte) parser.readByte(offset + 4);
    other = (byte) parser.readByte(offset + 5);
    shndx = parser.readHalf(offset + 6);
    value = parser.readLong(offset + 8);
    size = parser.readLong(offset + 16);
  }
}
