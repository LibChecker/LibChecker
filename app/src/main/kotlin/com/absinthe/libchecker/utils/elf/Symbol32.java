package com.absinthe.libchecker.utils.elf;

import java.io.IOException;

public class Symbol32 extends Elf.Symbol {
  public Symbol32(ElfParser parser, long offset) throws IOException {
    name = parser.readWord(offset);
    value = parser.readWord(offset + 4);
    size = parser.readWord(offset + 8);
    info = (byte) parser.readByte(offset + 12);
    other = (byte) parser.readByte(offset + 13);
    shndx = parser.readHalf(offset + 14);
  }
}
