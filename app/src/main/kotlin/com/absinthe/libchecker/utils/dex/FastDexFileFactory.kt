package com.absinthe.libchecker.utils.dex

import java.io.File
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import org.jf.dexlib2.iface.MultiDexContainer

interface FastDexFileFactory {

    fun loadDexContainer(
      file: File,
      opcodes: Opcodes
    ): MultiDexContainer<out DexBackedDexFile>

    class Base : FastDexFileFactory {

        override fun loadDexContainer(
            file: File,
            opcodes: Opcodes
        ): MultiDexContainer<out DexBackedDexFile> {
            if (!file.exists()) {
                throw DexFileFactory.DexFileNotFoundException("${file.name} does not exist")
            }
            return ZipDexContainer2(file, opcodes)
        }
    }
}
