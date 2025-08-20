package com.absinthe.libchecker.utils.dex

import com.absinthe.libchecker.compat.IZipFile
import com.absinthe.libchecker.compat.ZipFileCompat
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.ZipDexContainer
import com.android.tools.smali.dexlib2.iface.MultiDexContainer
import okio.Okio
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.regex.Pattern
import java.util.zip.ZipEntry

/**
 * Represents a zip file that contains dex files (i.e. an apk or jar file)
 */
class ZipDexContainer2(
    private val zipFilePath: File,
    private val opcodes: Opcodes?
) : MultiDexContainer<DexBackedDexFile> {

    companion object {
        private val DEX_PATTERN = Pattern.compile("(?<=classes)\\d*\\.dex$")
    }

    /**
     * Gets a list of the names of dex files in this zip file.
     *
     * @return A list of the names of dex files in this zip file
     */
    @Throws(IOException::class)
    override fun getDexEntryNames(): List<String> {
        val entryNames = mutableListOf<String>()
        getZipFile().use { zipFile ->
            val entriesEnumeration = zipFile.zipEntries

            while (entriesEnumeration.hasMoreElements()) {
                val entry = entriesEnumeration.nextElement()

                val name = entry.name
                if (!DEX_PATTERN.matcher(name).find()) {
                    continue
                }

                entryNames.add(name)
            }

            return entryNames
        }
    }

    /**
     * Loads a dex file from a specific named entry.
     *
     * @param entryName The name of the entry
     * @return A ZipDexFile, or null if there is no entry with the given name
     * @throws DexBackedDexFile.NotADexFile If the entry isn't a dex file
     */
    @Throws(IOException::class)
    override fun getEntry(entryName: String): MultiDexContainer.DexEntry<DexBackedDexFile>? {
        getZipFile().use { zipFile ->
            val entry = zipFile.getEntry(entryName) ?: return null
            return loadEntry(zipFile, entry)
        }
    }

    protected fun getZipFile(): IZipFile {
        return try {
            ZipFileCompat(zipFilePath)
        } catch (ex: Exception) {
            throw ZipDexContainer.NotAZipFileException()
        }
    }

    @Throws(IOException::class)
    protected fun loadEntry(zipFile: IZipFile, zipEntry: ZipEntry): MultiDexContainer.DexEntry<DexBackedDexFile> {
        zipFile.getInputStream(zipEntry).use { inputStream ->
            val buf = Okio.buffer(Okio.source(inputStream)).readByteArray()

            return object : MultiDexContainer.DexEntry<DexBackedDexFile> {
                override fun getEntryName(): String {
                    return zipEntry.name
                }

                override fun getDexFile(): DexBackedDexFile {
                    return DexBackedDexFile(opcodes, buf)
                }

                override fun getContainer(): MultiDexContainer<DexBackedDexFile> {
                    return this@ZipDexContainer2
                }
            }
        }
    }
}