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

public class Section32Header extends Elf.SectionHeader {
  public Section32Header(final ElfParser parser, final Elf.Header header, final int index)
    throws IOException {
    final long _offset = header.shoff + ((long) index * header.shentsize);
    type = parser.readWord(_offset + 0x4);
    offset = parser.readWord(_offset + 0x10);
    size = parser.readWord(_offset + 0x14);
    link = parser.readHalf(_offset + 0x18);
    info = parser.readWord(_offset + 0x1C);
    entsize = parser.readWord(_offset + 0x24);
  }
}
