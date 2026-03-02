package com.absinthe.libchecker.utils.dex;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.MultiDexContainer;

import java.io.File;
import java.io.IOException;


public class FastDexFileFactory {
  public static MultiDexContainer<? extends DexBackedDexFile> loadDexContainer(
    @NonNull File file, @Nullable final Opcodes opcodes) throws IOException {
    if (!file.exists()) {
      throw new DexFileFactory.DexFileNotFoundException("%s does not exist", file.getName());
    }

    return new ZipDexContainer2(file, opcodes);
  }
}
