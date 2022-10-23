package com.absinthe.libchecker.utils.elf

import com.absinthe.libchecker.annotation.ET_NOT_ELF
import java.io.InputStream

class ELF32EhdrParser {

  val e_ident = ByteArray(UNSIGNED_CHAR * EI_NIDENT)
  val e_type = ByteArray(ELF32_HALF)
  val e_machine = ByteArray(ELF32_HALF)
  val e_version = ByteArray(ELF32_WORD)
  val e_entry = ByteArray(ELF32_ADDR)
  val e_phoff = ByteArray(ELF32_OFF)
  val e_shoff = ByteArray(ELF32_OFF)
  val e_flags = ByteArray(ELF32_WORD)
  val e_ehsize = ByteArray(ELF32_HALF)
  val e_phentsize = ByteArray(ELF32_HALF)
  val e_phnum = ByteArray(ELF32_HALF)
  val e_shentsize = ByteArray(ELF32_HALF)
  val e_shnum = ByteArray(ELF32_HALF)
  val e_shstrndx = ByteArray(ELF32_HALF)

  private constructor()

  constructor(inputStream: InputStream) {
    parse(inputStream)
  }

  fun isElf(): Boolean {
    return e_ident[0].toInt() == 0x7F &&
      e_ident[1].toInt().toChar() == 'E' &&
      e_ident[2].toInt().toChar() == 'L' &&
      e_ident[3].toInt().toChar() == 'F'
  }

  fun getEType(): Int {
    if (!isElf()) {
      return ET_NOT_ELF
    }
    return e_type[0].toInt()
  }

  private fun parse(inputStream: InputStream) {
    inputStream.use {
      it.read(e_ident)
      it.read(e_type)
      it.read(e_machine)
      it.read(e_version)
      it.read(e_entry)
      it.read(e_phoff)
      it.read(e_shoff)
      it.read(e_flags)
      it.read(e_ehsize)
      it.read(e_phentsize)
      it.read(e_phnum)
      it.read(e_shentsize)
      it.read(e_shnum)
      it.read(e_shstrndx)
    }
  }

  companion object {
    // See also: https://docs.oracle.com/cd/E19683-01/816-1386/chapter6-43405/index.html
    private const val EI_NIDENT = 16
    private const val ELF32_ADDR = 4
    private const val ELF32_HALF = 2
    private const val ELF32_OFF = 4
    private const val ELF32_SWORD = 4
    private const val ELF32_WORD = 4
    private const val UNSIGNED_CHAR = 1

    // e_type
    private const val ET_NONE = 0
    private const val ET_REL = 1
    private const val ET_EXEC = 2
    private const val ET_DYN = 3
    private const val ET_CORE = 4
    private const val ET_LOPROC = 0xff00
    private const val ET_HIPROC = 0xffff

    // e_machine
    private const val EM_NONE = 0
    private const val EM_SPARC = 2
    private const val EM_386 = 3
    private const val EM_SPARC32PLUS = 18
    private const val EM_SPARCV9 = 43
  }
}
