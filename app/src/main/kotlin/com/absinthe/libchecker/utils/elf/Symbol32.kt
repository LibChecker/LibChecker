package com.absinthe.libchecker.utils.elf

import java.io.IOException

class Symbol32 @Throws(IOException::class) constructor(parser: ElfParser, offset: Long) : Elf.Symbol() {
  init {
    name = parser.readWord(offset)
    value = parser.readWord(offset + 4)
    size = parser.readWord(offset + 8)
    info = parser.readByte(offset + 12).toByte()
    other = parser.readByte(offset + 13).toByte()
    shndx = parser.readHalf(offset + 14)
  }
}