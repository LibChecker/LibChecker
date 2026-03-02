/**
 * Copyright 2015 - 2016 KeepSafe Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.absinthe.libchecker.utils.elf;

import java.io.IOException;

public class Elf32Header extends Elf.Header {
  private final ElfParser parser;

  public Elf32Header(final boolean bigEndian, final ElfParser parser) throws IOException {
    this.bigEndian = bigEndian;
    this.parser = parser;

    type = parser.readHalf(0x10);
    phoff = parser.readWord(0x1C);
    shoff = parser.readWord(0x20);
    phentsize = parser.readHalf(0x2A);
    phnum = parser.readHalf(0x2C);
    shentsize = parser.readHalf(0x2E);
    shnum = parser.readHalf(0x30);
    shstrndx = parser.readHalf(0x32);
  }

  @Override
  public Elf.SectionHeader getSectionHeader(final long index) throws IOException {
    return new Section32Header(parser, this, (int) index);
  }

  @Override
  public Elf.ProgramHeader getProgramHeader(final long index) throws IOException {
    return new Program32Header(parser, this, index);
  }

  @Override
  public Elf.DynamicStructure getDynamicStructure(final long baseOffset, final int index)
    throws IOException {
    return new Dynamic32Structure(parser, this, baseOffset, index);
  }

  @Override
  public Elf.Symbol getSymbol(long offset) throws IOException {
    return new Symbol32(parser, offset);
  }
}
