package com.absinthe.libchecker.utils.dex

import com.absinthe.libchecker.compat.IZipFile
import com.absinthe.libchecker.compat.ZipFileCompat
import com.google.common.io.ByteStreams
import java.io.File
import java.util.Enumeration
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import org.jf.dexlib2.dexbacked.ZipDexContainer.NotAZipFileException
import org.jf.dexlib2.iface.MultiDexContainer
import org.jf.dexlib2.iface.MultiDexContainer.DexEntry

/**
 * Represents a zip file that contains dex files (i.e. an apk or jar file)
 * Constructs a new ZipDexContainer for the given zip file
 *
 * @param zipFilePath The path to the zip file
 */
open class ZipDexContainer2(
    private val zipFilePath: File,
    private val opcodes: Opcodes,
): MultiDexContainer<DexBackedDexFile> {

   /**
    * Gets a list of the names of dex files in this zip file.
    *
    * @return A list of the names of dex files in this zip file
    */
    override fun getDexEntryNames(): MutableList<String> {
        val entryNames = ArrayList<String>()
        val zipFile: IZipFile = getZipFile()

        zipFile.use {
            val entriesEnumeration: Enumeration<out ZipEntry> = it.getZipEntries()

            while (entriesEnumeration.hasMoreElements()) {
                val entry = entriesEnumeration.nextElement()
                val name = entry.name

                if (!DEX_PATTERN.matcher(name).find()) {
                    continue
                }

                entryNames.add(name)
            }
        }

        return entryNames
    }

    /**
     * Loads a dex file from a specific named entry.
     *
     * @param entryName The name of the entry
     * @return A ZipDexFile, or null if there is no entry with the given name
     * @throws DexBackedDexFile.NotADexFile If the entry isn't a dex file
     */
    override fun getEntry(entryName: String): MultiDexContainer.DexEntry<DexBackedDexFile>? {
        val zipFile: IZipFile = getZipFile()
        zipFile.use {
            val entry: ZipEntry = it.getEntry(entryName) ?: return null
            return loadEntry(zipFile, entry)
        }
    }

    protected fun getZipFile(): IZipFile {
        return try {
            ZipFileCompat(zipFilePath)
        } catch (_: Exception) {
            throw NotAZipFileException()
        }
    }

    protected fun loadEntry(zipFile: IZipFile, zipEntry: ZipEntry): MultiDexContainer.DexEntry<DexBackedDexFile> {
        val inputStream = zipFile.getInputStream(zipEntry)

        inputStream.use {
            val buf = ByteStreams.toByteArray(inputStream)

            return object : DexEntry<DexBackedDexFile> {
                override fun getEntryName(): String {
                    return zipEntry.name
                }

                override fun getDexFile(): DexBackedDexFile {
                    return DexBackedDexFile(opcodes, buf)
                }

                override fun getContainer(): MultiDexContainer<out DexBackedDexFile> {
                    return this@ZipDexContainer2
                }
            }
        }
    }

    companion object {
        @JvmStatic
        private val DEX_PATTERN = Pattern.compile("(?<=classes)\\\\d*\\\\.dex\$")
    }
}
