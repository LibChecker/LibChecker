package com.absinthe.libchecker.utils.elf

import com.absinthe.libchecker.annotation.ET_NOT_ELF
import java.io.InputStream
import timber.log.Timber

class ELFParser(inputStream: InputStream) {

  val e_ident = ByteArray(EI_NIDENT)
  lateinit var e_type: ByteArray
  lateinit var e_machine: ByteArray
  lateinit var e_version: ByteArray
  lateinit var e_entry: ByteArray
  lateinit var e_phoff: ByteArray
  lateinit var e_shoff: ByteArray
  lateinit var e_flags: ByteArray
  lateinit var e_ehsize: ByteArray
  lateinit var e_phentsize: ByteArray
  lateinit var e_phnum: ByteArray
  lateinit var e_shentsize: ByteArray
  lateinit var e_shnum: ByteArray
  lateinit var e_shstrndx: ByteArray

  private lateinit var e_ident_entity: EIdent

  init {
    parse(inputStream)
  }

  fun isElf(): Boolean {
    return e_ident[0].toInt() == 0x7F &&
      e_ident[1].toInt().toChar() == 'E' &&
      e_ident[2].toInt().toChar() == 'L' &&
      e_ident[3].toInt().toChar() == 'F'
  }

  fun getEType(): Int {
    if (!isElf() || !this::e_type.isInitialized) {
      return ET_NOT_ELF
    }
    return e_type[0].toInt()
  }

  fun getEClass(): Int {
    if (!isElf() || !this::e_ident_entity.isInitialized) {
      return ET_NOT_ELF
    }
    return e_ident_entity.EI_CLASS[0].toInt()
  }

  private fun parse(inputStream: InputStream) {
    inputStream.use {
      it.read(e_ident)
      e_ident_entity = EIdent(e_ident)

      if (e_ident_entity.EI_CLASS[0].toInt() == EIdent.ELFCLASS32) {
        e_type = ByteArray(ELF32_HALF)
        e_machine = ByteArray(ELF32_HALF)
        e_version = ByteArray(ELF32_WORD)
        e_entry = ByteArray(ELF32_ADDR)
        e_phoff = ByteArray(ELF32_OFF)
        e_shoff = ByteArray(ELF32_OFF)
        e_flags = ByteArray(ELF32_WORD)
        e_ehsize = ByteArray(ELF32_HALF)
        e_phentsize = ByteArray(ELF32_HALF)
        e_phnum = ByteArray(ELF32_HALF)
        e_shentsize = ByteArray(ELF32_HALF)
        e_shnum = ByteArray(ELF32_HALF)
        e_shstrndx = ByteArray(ELF32_HALF)
      } else if (e_ident_entity.EI_CLASS[0].toInt() == EIdent.ELFCLASS64) {
        e_type = ByteArray(ELF64_HALF)
        e_machine = ByteArray(ELF64_HALF)
        e_version = ByteArray(ELF64_WORD)
        e_entry = ByteArray(ELF64_ADDR)
        e_phoff = ByteArray(ELF64_OFF)
        e_shoff = ByteArray(ELF64_OFF)
        e_flags = ByteArray(ELF64_WORD)
        e_ehsize = ByteArray(ELF64_HALF)
        e_phentsize = ByteArray(ELF64_HALF)
        e_phnum = ByteArray(ELF64_HALF)
        e_shentsize = ByteArray(ELF64_HALF)
        e_shnum = ByteArray(ELF64_HALF)
        e_shstrndx = ByteArray(ELF64_HALF)
      } else {
        Timber.w("Not a valid ELF file")
        return@use
      }
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

  class EIdent(array: ByteArray) {
    val EI_MAG0 = ByteArray(1)
    val EI_MAG1 = ByteArray(1)
    val EI_MAG2 = ByteArray(1)
    val EI_MAG3 = ByteArray(1)
    val EI_CLASS = ByteArray(1)
    val EI_DATA = ByteArray(1)
    val EI_VERSION = ByteArray(1)
    val EI_OSABI = ByteArray(1)
    val EI_ABIVERSION = ByteArray(1)
    val EI_PAD = ByteArray(6)
    val EI_NIDENT = ByteArray(1)

    init {
      EI_MAG0[0] = array[0]
      EI_MAG1[0] = array[1]
      EI_MAG2[0] = array[2]
      EI_MAG3[0] = array[3]
      EI_CLASS[0] = array[4]
      EI_DATA[0] = array[5]
      EI_VERSION[0] = array[6]
      EI_OSABI[0] = array[7]
      EI_ABIVERSION[0] = array[8]
      EI_PAD[0] = array[9]
      EI_PAD[1] = array[10]
      EI_PAD[2] = array[11]
      EI_PAD[3] = array[12]
      EI_PAD[4] = array[13]
      EI_PAD[5] = array[14]
      EI_NIDENT[0] = array[15]
    }

    companion object {
      const val ELFCLASSNONE = 0
      const val ELFCLASS32 = 1
      const val ELFCLASS64 = 2
    }
  }

  companion object {
    // See also: https://docs.oracle.com/cd/E19683-01/816-1386/chapter6-43405/index.html
    // e_type
    private const val ET_NONE = 0
    private const val ET_REL = 1
    private const val ET_EXEC = 2
    private const val ET_DYN = 3
    private const val ET_CORE = 4
    private const val ET_LOPROC = 0xff00
    private const val ET_HIPROC = 0xffff

    private const val EI_NIDENT = 16
    private const val UNSIGNED_CHAR = 1

    // elf32 offset
    private const val ELF32_ADDR = 4
    private const val ELF32_HALF = 2
    private const val ELF32_OFF = 4
    private const val ELF32_SWORD = 4
    private const val ELF32_WORD = 4

    // elf64 offset
    private const val ELF64_ADDR = 8
    private const val ELF64_HALF = 2
    private const val ELF64_OFF = 8
    private const val ELF64_SWORD = 4
    private const val ELF64_WORD = 4
    private const val ELF64_XWORD = 8
    private const val ELF64_SXWORD = 8
  }
}
