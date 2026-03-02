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

public class Program32Header extends Elf.ProgramHeader {
  public Program32Header(final ElfParser parser, final Elf.Header header, final long index)
    throws IOException {
    final long baseOffset = header.phoff + (index * header.phentsize);
    type = parser.readWord(baseOffset);
    offset = parser.readWord(baseOffset + 0x4);
    vaddr = parser.readWord(baseOffset + 0x8);
    memsz = parser.readWord(baseOffset + 0x14);
    align = parser.readWord(baseOffset + 0x1C);
  }
}
