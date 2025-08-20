package com.absinthe.libchecker.utils.dex

import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.MultiDexContainer
import java.io.File
import java.io.IOException

class FastDexFileFactory {
    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun loadDexContainer(
            file: File,
            opcodes: Opcodes?
        ): MultiDexContainer<out DexBackedDexFile> {
            if (!file.exists()) {
                throw DexFileFactory.DexFileNotFoundException("%s does not exist", file.name)
            }

            return ZipDexContainer2(file, opcodes)
        }
    }
}