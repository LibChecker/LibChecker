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

class Elf32Header @Throws(IOException::class) constructor(
  bigEndian: Boolean,
  private val parser: ElfParser
) : Elf.Header() {

  init {
    this.bigEndian = bigEndian
    type = parser.readHalf(0x10)
    phoff = parser.readWord(0x1C)
    shoff = parser.readWord(0x20)
    phentsize = parser.readHalf(0x2A)
    phnum = parser.readHalf(0x2C)
    shentsize = parser.readHalf(0x2E)
    shnum = parser.readHalf(0x30)
    shstrndx = parser.readHalf(0x32)
  }

  @Throws(IOException::class)
  override fun getSectionHeader(index: Long): Elf.SectionHeader {
    return Section32Header(parser, this, index.toInt())
  }

  @Throws(IOException::class)
  override fun getProgramHeader(index: Long): Elf.ProgramHeader {
    return Program32Header(parser, this, index)
  }

  @Throws(IOException::class)
  override fun getDynamicStructure(baseOffset: Long, index: Int): Elf.DynamicStructure {
    return Dynamic32Structure(parser, this, baseOffset, index)
  }

  @Throws(IOException::class)
  override fun getSymbol(offset: Long): Elf.Symbol {
    return Symbol32(parser, offset)
  }
}