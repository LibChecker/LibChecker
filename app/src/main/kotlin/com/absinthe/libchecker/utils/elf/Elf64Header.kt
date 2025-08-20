/**
 * Copyright 2015 - 2016 KeepSafe Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.absinthe.libchecker.utils.elf

import java.io.IOException

class Elf64Header @Throws(IOException::class) constructor(
  bigEndian: Boolean,
  private val parser: ElfParser
) : Elf.Header() {

  init {
    this.bigEndian = bigEndian
    type = parser.readHalf(0x10)
    phoff = parser.readLong(0x20)
    shoff = parser.readLong(0x28)
    phentsize = parser.readHalf(0x36)
    phnum = parser.readHalf(0x38)
    shentsize = parser.readHalf(0x3A)
    shnum = parser.readHalf(0x3C)
    shstrndx = parser.readHalf(0x3E)
  }

  @Throws(IOException::class)
  override fun getSectionHeader(index: Long): Elf.SectionHeader {
    return Section64Header(parser, this, index)
  }

  @Throws(IOException::class)
  override fun getProgramHeader(index: Long): Elf.ProgramHeader {
    return Program64Header(parser, this, index)
  }

  @Throws(IOException::class)
  override fun getDynamicStructure(baseOffset: Long, index: Int): Elf.DynamicStructure {
    return Dynamic64Structure(parser, this, baseOffset, index)
  }

  @Throws(IOException::class)
  override fun getSymbol(offset: Long): Elf.Symbol {
    return Symbol64(parser, offset)
  }
}