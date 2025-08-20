package com.absinthe.libchecker.utils.manifest

import androidx.collection.ArrayMap
import com.absinthe.libchecker.compat.IZipFile
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.features.applist.detail.bean.StaticLibItem
import pxb.android.Res_value
import pxb.android.axml.AxmlReader
import pxb.android.axml.AxmlVisitor
import pxb.android.axml.NodeVisitor
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

class StaticLibraryReader private constructor(apk: File) {
    private val staticLibs = ArrayMap<String, StaticLibItem>()

    init {
        try {
            ZipFileCompat(apk).use { zip ->
                val inputStream = zip.getInputStream(zip.getEntry("AndroidManifest.xml"))
                val bytes = getBytesFromInputStream(inputStream)
                val reader = AxmlReader(bytes ?: ByteArray(0))
                reader.accept(object : AxmlVisitor() {
                    override fun child(ns: String?, name: String?): NodeVisitor {
                        val child = super.child(ns, name)
                        return ManifestTagVisitor(child)
                    }
                })
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun getStaticLibrary(apk: File): Map<String, StaticLibItem> {
            return StaticLibraryReader(apk).staticLibs
        }

        @JvmStatic
        fun getBytesFromInputStream(inputStream: InputStream?): ByteArray? {
            if (inputStream == null) return null
            
            return try {
                ByteArrayOutputStream().use { bos ->
                    val b = ByteArray(1024)
                    var n: Int
                    while (inputStream.read(b).also { n = it } != -1) {
                        bos.write(b, 0, n)
                    }
                    bos.toByteArray()
                }
            } catch (e: Exception) {
                Timber.w(e)
                null
            }
        }

        @JvmStatic
        fun extractIntPart(str: String): Int {
            var result = 0
            val length = str.length
            for (offset in 0 until length) {
                val c = str[offset]
                if (c in '0'..'9') {
                    result = result * 10 + (c - '0')
                } else {
                    break
                }
            }
            return result
        }
    }

    private inner class ManifestTagVisitor(child: NodeVisitor?) : NodeVisitor(child) {
        override fun child(ns: String?, name: String?): NodeVisitor {
            val child = super.child(ns, name)
            return if ("application" == name) {
                ApplicationTagVisitor(child)
            } else child
        }

        private inner class ApplicationTagVisitor(child: NodeVisitor?) : NodeVisitor(child) {
            override fun child(ns: String?, name: String?): NodeVisitor {
                val child = super.child(ns, name)
                return if ("uses-static-library" == name) {
                    StaticLibraryVisitor(child)
                } else child
            }
        }
    }

    private inner class StaticLibraryVisitor(child: NodeVisitor?) : NodeVisitor(child) {
        var name: String? = null
        var version: Int? = null
        var certDigest: String? = null

        override fun attr(ns: String?, name: String?, resourceId: Int, raw: String?, value: Res_value?) {
            if ("name" == name && value?.type == Res_value.TYPE_STRING) {
                this.name = value.toString()
            } else if ("version" == name && value?.type == Res_value.TYPE_INT_DEC) {
                version = value.data
            } else if ("certDigest" == name && value?.type == Res_value.TYPE_STRING) {
                this.certDigest = value.toString()
            }
            super.attr(ns, name, resourceId, raw, value)
        }

        override fun end() {
            val libName = name
            val libVersion = version
            if (libName != null && libVersion != null) {
                val item = StaticLibItem(libName, libVersion, certDigest, "")
                staticLibs[libName] = item
            }
            super.end()
        }
    }
}