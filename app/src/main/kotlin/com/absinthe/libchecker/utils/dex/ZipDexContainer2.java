package com.absinthe.libchecker.utils.dex;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.MultiDexContainer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a zip file that contains dex files (i.e. an apk or jar file)
 */
public class ZipDexContainer2 implements MultiDexContainer<DexBackedDexFile> {

  private final File zipFilePath;
  @Nullable
  private final Opcodes opcodes;
  private static final Pattern DEX_PATTERN = Pattern.compile("(?<=classes)\\d*\\.dex$");

  /**
   * Constructs a new ZipDexContainer for the given zip file
   *
   * @param zipFilePath The path to the zip file
   */
  public ZipDexContainer2(@Nonnull File zipFilePath, @Nullable Opcodes opcodes) {
    this.zipFilePath = zipFilePath;
    this.opcodes = opcodes;
  }

  /**
   * Gets a list of the names of dex files in this zip file.
   *
   * @return A list of the names of dex files in this zip file
   */
  @Nonnull
  @Override
  public List<String> getDexEntryNames() throws IOException {
    List<String> entryNames = Lists.newArrayList();
    ZipFile zipFile = getZipFile();
    try {
      Enumeration<? extends ZipEntry> entriesEnumeration = zipFile.entries();

      while (entriesEnumeration.hasMoreElements()) {
        ZipEntry entry = entriesEnumeration.nextElement();

        String name = entry.getName();
        if (!DEX_PATTERN.matcher(name).find()) {
          continue;
        }

        entryNames.add(name);
      }

      return entryNames;
    } finally {
      zipFile.close();
    }
  }

  /**
   * Loads a dex file from a specific named entry.
   *
   * @param entryName The name of the entry
   * @return A ZipDexFile, or null if there is no entry with the given name
   * @throws DexBackedDexFile.NotADexFile If the entry isn't a dex file
   */
  @Nullable
  @Override
  public DexEntry<DexBackedDexFile> getEntry(@Nonnull String entryName) throws IOException {
    ZipFile zipFile = getZipFile();
    try {
      ZipEntry entry = zipFile.getEntry(entryName);
      if (entry == null) {
        return null;
      }

      return loadEntry(zipFile, entry);
    } finally {
      zipFile.close();
    }
  }

  protected ZipFile getZipFile() throws IOException {
    try {
      return new ZipFile(zipFilePath);
    } catch (IOException ex) {
      throw new org.jf.dexlib2.dexbacked.ZipDexContainer.NotAZipFileException();
    }
  }

  @Nonnull
  protected DexEntry loadEntry(@Nonnull ZipFile zipFile, @Nonnull ZipEntry zipEntry) throws IOException {
    InputStream inputStream = zipFile.getInputStream(zipEntry);
    try {
      byte[] buf = ByteStreams.toByteArray(inputStream);

      return new DexEntry() {
        @Nonnull
        @Override
        public String getEntryName() {
          return zipEntry.getName();
        }

        @Nonnull
        @Override
        public DexFile getDexFile() {
          return new DexBackedDexFile(opcodes, buf);
        }

        @Nonnull
        @Override
        public MultiDexContainer getContainer() {
          return ZipDexContainer2.this;
        }
      };
    } finally {
      inputStream.close();
    }
  }
}
