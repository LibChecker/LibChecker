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

class Dynamic64Structure @Throws(IOException::class) constructor(
  parser: ElfParser,
  header: Elf.Header,
  baseOffset: Long,
  index: Int
) : Elf.DynamicStructure() {
  init {
    val offset = baseOffset + (index * 16L)
    tag = parser.readLong(offset)
    `val` = parser.readLong(offset + 0x8)
  }
}