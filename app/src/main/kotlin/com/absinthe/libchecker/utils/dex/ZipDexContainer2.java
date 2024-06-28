package com.absinthe.libchecker.utils.dex;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.absinthe.libchecker.compat.IZipFile;
import com.absinthe.libchecker.compat.ZipFileCompat;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.dexbacked.ZipDexContainer;
import com.android.tools.smali.dexlib2.iface.MultiDexContainer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import okio.Okio;

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
  public ZipDexContainer2(@NonNull File zipFilePath, @Nullable Opcodes opcodes) {
    this.zipFilePath = zipFilePath;
    this.opcodes = opcodes;
  }

  /**
   * Gets a list of the names of dex files in this zip file.
   *
   * @return A list of the names of dex files in this zip file
   */
  @NonNull
  @Override
  public List<String> getDexEntryNames() throws IOException {
    List<String> entryNames = new ArrayList<>();
    try (IZipFile zipFile = getZipFile()) {
      Enumeration<? extends ZipEntry> entriesEnumeration = zipFile.getZipEntries();

      while (entriesEnumeration.hasMoreElements()) {
        ZipEntry entry = entriesEnumeration.nextElement();

        String name = entry.getName();
        if (!DEX_PATTERN.matcher(name).find()) {
          continue;
        }

        entryNames.add(name);
      }

      return entryNames;
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
  public DexEntry<DexBackedDexFile> getEntry(@NonNull String entryName) throws IOException {
    try (IZipFile zipFile = getZipFile()) {
      ZipEntry entry = zipFile.getEntry(entryName);
      if (entry == null) {
        return null;
      }

      return loadEntry(zipFile, entry);
    }
  }

  protected IZipFile getZipFile() {
    try {
      return new ZipFileCompat(zipFilePath);
    } catch (Exception ex) {
      throw new ZipDexContainer.NotAZipFileException();
    }
  }

  @NonNull
  protected DexEntry<DexBackedDexFile> loadEntry(@NonNull IZipFile zipFile, @NonNull ZipEntry zipEntry) throws IOException {
    try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
      byte[] buf = Okio.buffer(Okio.source(inputStream)).readByteArray();

      return new DexEntry<>() {
        @NonNull
        @Override
        public String getEntryName() {
          return zipEntry.getName();
        }

        @NonNull
        @Override
        public DexBackedDexFile getDexFile() {
          return new DexBackedDexFile(opcodes, buf);
        }

        @NonNull
        @Override
        public MultiDexContainer<DexBackedDexFile> getContainer() {
          return ZipDexContainer2.this;
        }
      };
    }
  }
}
