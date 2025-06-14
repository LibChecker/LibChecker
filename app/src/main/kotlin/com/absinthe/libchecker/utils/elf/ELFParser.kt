package com.absinthe.libchecker.utils.elf

import com.absinthe.libchecker.annotation.ET_NOT_ELF
import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ELFParser(inputStream: InputStream) {

  lateinit var e_ident: EIdent
  var e_type: Short = 0
  var e_machine: Short = 0
  var e_version: Int = 0
  var e_entry: Long = 0
  var e_phoff: Long = 0
  var e_shoff: Long = 0
  var e_flags: Int = 0
  var e_ehsize: Short = 0
  var e_phentsize: Short = 0
  var e_phnum: Short = 0
  var e_shentsize: Short = 0
  var e_shnum: Short = 0
  var e_shstrndx: Short = 0
  val programHeaders: MutableList<ProgramHeader> = mutableListOf()

  var readBytes: Long = 0
    private set

  init {
    parse(inputStream)
  }

  fun isElf(): Boolean {
    return e_ident.EI_MAG0 == 0x7F.toByte() &&
      e_ident.EI_MAG1.toInt().toChar() == 'E' &&
      e_ident.EI_MAG2.toInt().toChar() == 'L' &&
      e_ident.EI_MAG3.toInt().toChar() == 'F'
  }

  fun getEType(): Int {
    if (!isElf()) {
      return ET_NOT_ELF
    }
    return e_type.toInt()
  }

  fun getEClass(): Int {
    if (!isElf()) {
      return ET_NOT_ELF
    }
    return e_ident.EI_CLASS.toInt()
  }

  fun getMinPageSize(): Int {
    var minAlign: Long? = null
    programHeaders.forEach {
      if (it.p_type == ProgramHeader.PT_LOAD) {
        minAlign = minAlign?.coerceAtMost(it.p_align) ?: it.p_align
      }
    }
    return minAlign?.toInt() ?: -1
  }

  private fun parse(inputStream: InputStream) {
    inputStream.use {
      val e_ident_array = ByteArray(EI_NIDENT)
      readFully(it, e_ident_array, EI_NIDENT)
      readBytes += EI_NIDENT
      e_ident = EIdent(e_ident_array)

      if (!isElf()) {
        return
      }

      val ehSize = when (getEClass()) {
        EIdent.ELFCLASS32 -> 36 // 32-bit ELF header size
        EIdent.ELFCLASS64 -> 48 // 64-bit ELF header size
        else -> return
      }
      val byteOrder =
        if (e_ident.EI_DATA.toInt() == EIdent.ELFDATA2MSB) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
      val buffer = ByteBuffer.allocate(ehSize).order(byteOrder)
      readFully(it, buffer.array(), ehSize)
      readBytes += ehSize

      when (getEClass()) {
        EIdent.ELFCLASS32 -> {
          e_type = buffer.short
          e_machine = buffer.short
          e_version = buffer.int
          e_entry = buffer.int.toLong() and 0xFFFFFFFFL
          e_phoff = buffer.int.toLong() and 0xFFFFFFFFL
          e_shoff = buffer.int.toLong() and 0xFFFFFFFFL
          e_flags = buffer.int
          e_ehsize = buffer.short
          e_phentsize = buffer.short
          e_phnum = buffer.short
          e_shentsize = buffer.short
          e_shnum = buffer.short
          e_shstrndx = buffer.short
        }

        EIdent.ELFCLASS64 -> {
          e_type = buffer.short
          e_machine = buffer.short
          e_version = buffer.int
          e_entry = buffer.long
          e_phoff = buffer.long
          e_shoff = buffer.long
          e_flags = buffer.int
          e_ehsize = buffer.short
          e_phentsize = buffer.short
          e_phnum = buffer.short
          e_shentsize = buffer.short
          e_shnum = buffer.short
          e_shstrndx = buffer.short
        }

        else -> {
          return
        }
      }

      // Program Headers
      if (e_phoff > 0 && e_phnum > 0) {
        for (i in 0 until e_phnum) {
          val phBuffer = ByteBuffer.allocate(e_phentsize.toInt()).order(byteOrder)
          readFully(it, phBuffer.array(), e_phentsize.toInt())
          readBytes += e_phentsize.toInt()

          val programHeader = if (getEClass() == EIdent.ELFCLASS32) {
            ProgramHeader(
              p_type = phBuffer.int,
              p_offset = phBuffer.int.toLong(),
              p_vaddr = phBuffer.int.toLong(),
              p_paddr = phBuffer.int.toLong(),
              p_filesz = phBuffer.int.toLong(),
              p_memsz = phBuffer.int.toLong(),
              p_flags = phBuffer.int,
              p_align = phBuffer.int.toLong()
            )
          } else {
            ProgramHeader(
              p_type = phBuffer.int,
              p_flags = phBuffer.int,
              p_offset = phBuffer.long,
              p_vaddr = phBuffer.long,
              p_paddr = phBuffer.long,
              p_filesz = phBuffer.long,
              p_memsz = phBuffer.long,
              p_align = phBuffer.long
            )
          }
          programHeaders.add(programHeader)
        }
      }
    }
  }

  private fun readFully(inputStream: InputStream, buffer: ByteArray, length: Int) {
    var bytesRead = 0

    // Read in a loop until the buffer is fully filled
    while (bytesRead < length) {
      val readCount = inputStream.read(buffer, bytesRead, length - bytesRead)
      if (readCount == -1) {
        throw EOFException("Unexpected end of stream while reading $length bytes")
      }
      bytesRead += readCount
    }
  }

  class EIdent(array: ByteArray) {
    val EI_MAG0 = array[0]
    val EI_MAG1 = array[1]
    val EI_MAG2 = array[2]
    val EI_MAG3 = array[3]
    val EI_CLASS = array[4]
    val EI_DATA = array[5]
    val EI_VERSION = array[6]
    val EI_OSABI = array[7]
    val EI_ABIVERSION = array[8]
    val EI_PAD = array.sliceArray(9..14)
    val EI_NIDENT = array[15]

    companion object {
      const val ELFCLASSNONE = 0
      const val ELFCLASS32 = 1
      const val ELFCLASS64 = 2
      const val ELFDATA2MSB = 2
    }
  }

  data class ProgramHeader(
    val p_type: Int,
    val p_offset: Long,
    val p_vaddr: Long,
    val p_paddr: Long,
    val p_filesz: Long,
    val p_memsz: Long,
    val p_flags: Int,
    val p_align: Long
  ) {
    companion object {
      const val PT_LOAD = 1
    }
  }

  companion object {
    private const val EI_NIDENT = 16

    // ELF32 constants
    private const val ELF32_ADDR = 4
    private const val ELF32_HALF = 2
    private const val ELF32_OFF = 4
    private const val ELF32_WORD = 4

    // ELF64 constants
    private const val ELF64_ADDR = 8
    private const val ELF64_HALF = 2
    private const val ELF64_OFF = 8
    private const val ELF64_WORD = 4
    private const val ELF64_XWORD = 8
  }
}
