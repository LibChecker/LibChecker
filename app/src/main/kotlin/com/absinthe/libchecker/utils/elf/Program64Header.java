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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Program64Header extends Elf.ProgramHeader {
  public Program64Header(final ElfParser parser, final Elf.Header header, final long index)
    throws IOException {
    final long baseOffset = header.phoff + (index * header.phentsize);
    type = parser.readWord(baseOffset);
    offset = parser.readLong(baseOffset + 0x8);
    vaddr = parser.readLong(baseOffset + 0x10);
    memsz = parser.readLong(baseOffset + 0x28);
    align = parser.readLong(baseOffset + 0x30);
  }
}
