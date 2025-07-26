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

import static com.absinthe.libchecker.annotation.ElfTypeKt.ET_NOT_ELF;

import com.absinthe.libchecker.utils.elf.source.DataSource;
import com.absinthe.libchecker.utils.elf.source.FileChannelDataSource;
import com.absinthe.libchecker.utils.elf.source.InputStreamDataSource;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class ElfParser implements Closeable, Elf {
  private final DataSource dataSource;
  private Elf.Header mHeader;

  public ElfParser(final File file) throws IOException {
    if (file == null || !file.exists()) {
      throw new IllegalArgumentException("File is null or does not exist");
    }

    final FileInputStream inputStream = new FileInputStream(file);
    this.dataSource = new FileChannelDataSource(inputStream.getChannel());
  }

  public ElfParser(final InputStream is) throws IOException {
    this.dataSource = new InputStreamDataSource(is);
  }

  public int getEType() {
    if (mHeader == null) {
      try {
        mHeader = parseHeader();
      } catch (IOException e) {
        Timber.d("IOException: %s", e.getMessage());
        return ET_NOT_ELF;
      }
    }
    return mHeader.type;
  }

  public int getMinPageSize() {
    if (mHeader == null) {
      try {
        mHeader = parseHeader();
      } catch (IOException e) {
        return 0;
      }
    }
    if (mHeader.phnum == 0) {
      return -1; // No program headers
    }

    long minAlign = Long.MAX_VALUE;
    for (long i = 0; i < mHeader.phnum; ++i) {
      final ProgramHeader ph;
      try {
        ph = mHeader.getProgramHeader(i);
      } catch (IOException e) {
        continue;
      }
      if (ph.type == ProgramHeader.PT_LOAD) {
        minAlign = Math.min(minAlign, ph.align);
      }
    }

    return minAlign == Long.MAX_VALUE ? -1 : (int) minAlign;
  }

  public Elf.Header parseHeader() throws IOException {
    if (mHeader != null) {
      return mHeader;
    }

    dataSource.position(0);

    final int MAGIC = 0x464C457F;
    dataSource.setOrder(ByteOrder.LITTLE_ENDIAN);
    long magic = dataSource.readWord(0);

    if (magic != MAGIC) {
      throw new IOException("Invalid ELF magic");
    }

    final short elf_class = dataSource.readByte(4);
    final short elf_byte_order = dataSource.readByte(5);

    dataSource.position(0);

    boolean isBigEndian = elf_byte_order == Header.ELFDATA2MSB;
    dataSource.setOrder(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

    if (elf_class == Elf.Header.ELFCLASS32) {
      mHeader = new Elf32Header(isBigEndian, this);
    } else if (elf_class == Elf.Header.ELFCLASS64) {
      mHeader = new Elf64Header(isBigEndian, this);
    } else {
      throw new IOException("Invalid ELF class: " + elf_class);
    }

    return mHeader;
  }

  public List<String> parseNeededDependencies() throws IOException {
    dataSource.position(0);
    final List<String> dependencies = new ArrayList<>();
    if (mHeader == null) {
      mHeader = parseHeader();
    }
    final Elf.Header header = mHeader;

    long numProgramHeaderEntries = header.phnum;
    if (numProgramHeaderEntries == 0xFFFF) {
      /**
       * Extended Numbering
       *
       * If the real number of program header table entries is larger than
       * or equal to PN_XNUM(0xffff), it is set to sh_info field of the
       * section header at index 0, and PN_XNUM is set to e_phnum
       * field. Otherwise, the section header at index 0 is zero
       * initialized, if it exists.
       **/
      final Elf.SectionHeader sectionHeader = header.getSectionHeader(0);
      numProgramHeaderEntries = sectionHeader.info;
    }

    long dynamicSectionOff = 0;
    for (long i = 0; i < numProgramHeaderEntries; ++i) {
      final Elf.ProgramHeader programHeader = header.getProgramHeader(i);
      if (programHeader.type == ProgramHeader.PT_DYNAMIC) {
        dynamicSectionOff = programHeader.offset;
        break;
      }
    }

    if (dynamicSectionOff == 0) {
      // No dynamic linking info, nothing to load
      return Collections.unmodifiableList(dependencies);
    }

    int i = 0;
    final List<Long> neededOffsets = new ArrayList<>();
    long vStringTableOff = 0;
    Elf.DynamicStructure dynStructure;
    do {
      dynStructure = header.getDynamicStructure(dynamicSectionOff, i);
      if (dynStructure.tag == DynamicStructure.DT_NEEDED) {
        neededOffsets.add(dynStructure.val);
      } else if (dynStructure.tag == DynamicStructure.DT_STRTAB) {
        vStringTableOff = dynStructure.val; // d_ptr union
      }
      ++i;
    } while (dynStructure.tag != DynamicStructure.DT_NULL);

    if (vStringTableOff == 0) {
      throw new IllegalStateException("String table offset not found!");
    }

    // Map to file offset
    final long stringTableOff = offsetFromVma(header, numProgramHeaderEntries, vStringTableOff);
    for (final Long strOff : neededOffsets) {
      dependencies.add(dataSource.readString(stringTableOff + strOff));
    }

    return dependencies;
  }

  private long offsetFromVma(final Elf.Header header, final long numEntries, final long vma)
    throws IOException {
    for (long i = 0; i < numEntries; ++i) {
      final Elf.ProgramHeader programHeader = header.getProgramHeader(i);
      if (programHeader.type == ProgramHeader.PT_LOAD) {
        // Within memsz instead of filesz to be more tolerant
        if (programHeader.vaddr <= vma
          && vma <= programHeader.vaddr + programHeader.memsz) {
          return vma - programHeader.vaddr + programHeader.offset;
        }
      }
    }

    throw new IllegalStateException("Could not map vma to file offset!");
  }

  public List<String> parseEntryPoints() throws IOException {
    long symtabOff = 0;
    long symtabSize = 0;
    long symtabEntSize = 0;
    long strtabOff = 0;

    if (mHeader == null) {
      mHeader = parseHeader();
    }

    for (int i = 0; i < mHeader.shnum; ++i) {
      final Elf.SectionHeader sh = mHeader.getSectionHeader(i);
      if (sh.type == Elf.SectionHeader.SHT_SYMTAB || sh.type == Elf.SectionHeader.SHT_DYNSYM) {
        symtabOff = sh.offset;
        symtabSize = sh.size;
        symtabEntSize = sh.entsize;

        final Elf.SectionHeader strtabSh = mHeader.getSectionHeader(sh.link);
        strtabOff = strtabSh.offset;
        break;
      }
    }

    if (symtabOff == 0) {
      return Collections.emptyList();
    }

    final List<String> entryPoints = new ArrayList<>();
    final long numSymbols = symtabSize / symtabEntSize;
    for (long i = 0; i < numSymbols; ++i) {
      final Elf.Symbol symbol = mHeader.getSymbol(symtabOff + i * symtabEntSize);
      if (symbol.getType() == Elf.Symbol.STT_FUNC &&
        symbol.getBinding() == Elf.Symbol.STB_GLOBAL &&
        symbol.shndx != Elf.SectionHeader.SHN_UNDEF &&
        symbol.name != 0) {
        String symbolName = dataSource.readString(strtabOff + symbol.name);
        if (symbolName.startsWith("JNI_") || symbolName.startsWith("Java_")) {
          entryPoints.add(symbolName);
        }
      }
    }

    return entryPoints;
  }

  /**
   * Reads bytes from the ELF file into the given buffer.
   *
   * @param b      The buffer to read bytes into.
   * @param offset The offset in the ELF file to read bytes from.
   * @throws IOException If an I/O error occurs.
   */
  void read(final ByteBuffer b, final long offset) throws IOException {
    dataSource.read(b, offset);
  }

  /**
   * Reads a string from the ELF file.
   *
   * @param offset The offset in the ELF file to read the string from.
   * @return The read string.
   * @throws IOException If an I/O error occurs.
   */
  protected String readString(long offset) throws IOException {
    return dataSource.readString(offset);
  }

  /**
   * Reads a long value from the ELF file.
   *
   * @param offset The offset in the ELF file to read the long value from.
   * @return The read long value.
   * @throws IOException If an I/O error occurs.
   */
  protected long readLong(final long offset) throws IOException {
    return dataSource.readLong(offset);
  }

  /**
   * Reads a word (32 bits) value from the ELF file.
   *
   * @param offset The offset in the ELF file to read the word value from.
   * @return The read word value.
   * @throws IOException If an I/O error occurs.
   */
  protected long readWord(final long offset) throws IOException {
    return dataSource.readWord(offset);
  }

  /**
   * Reads a half-word (16 bits) value from the ELF file.
   *
   * @param offset The offset in the ELF file to read the half-word value from.
   * @return The read half-word value.
   * @throws IOException If an I/O error occurs.
   */
  protected int readHalf(final long offset) throws IOException {
    return dataSource.readHalf(offset);
  }

  /**
   * Reads a byte value from the ELF file.
   *
   * @param offset The offset in the ELF file to read the byte value from.
   * @return The read byte value.
   * @throws IOException If an I/O error occurs.
   */
  protected short readByte(final long offset) throws IOException {
    return dataSource.readByte(offset);
  }

  /**
   * Reads bytes from the ELF file.
   *
   * @param buf    The buffer to read bytes into.
   * @param offset The offset in the ELF file to read bytes from.
   * @param length The number of bytes to read.
   * @throws IOException If an I/O error occurs.
   */
  protected void read(long offset, byte[] buf, final int length) throws IOException {
    dataSource.read(offset, buf, length);
  }

  @Override
  public void close() throws IOException {
    dataSource.close();
  }
}
