package com.absinthe.libchecker.utils.elf

import java.io.IOException

class Symbol64 @Throws(IOException::class) constructor(parser: ElfParser, offset: Long) : Elf.Symbol() {
  init {
    name = parser.readWord(offset)
    info = parser.readByte(offset + 4).toByte()
    other = parser.readByte(offset + 5).toByte()
    shndx = parser.readHalf(offset + 6)
    value = parser.readLong(offset + 8)
    size = parser.readLong(offset + 16)
  }
}