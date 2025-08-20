package com.absinthe.libchecker.utils.manifest

import androidx.collection.ArrayMap
import com.absinthe.libchecker.compat.IZipFile
import com.absinthe.libchecker.compat.ZipFileCompat
import pxb.android.Res_value
import pxb.android.axml.AxmlReader
import pxb.android.axml.AxmlVisitor
import pxb.android.axml.NodeVisitor
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

class HiddenPermissionsReader private constructor(apk: File) {
    private val permissionMap = ArrayMap<String, Any>()

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
        fun getHiddenPermissions(apk: File): Map<String, Any> {
            return HiddenPermissionsReader(apk).permissionMap
        }

        private fun getBytesFromInputStream(inputStream: InputStream?): ByteArray? {
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
    }

    private inner class ManifestTagVisitor(child: NodeVisitor?) : NodeVisitor(child) {
        override fun child(ns: String?, name: String?): NodeVisitor {
            val child = super.child(ns, name)
            return if ("uses-permission" == name) {
                PermissionVisitor(child)
            } else child
        }
    }

    private inner class PermissionVisitor(child: NodeVisitor?) : NodeVisitor(child) {
        var name: String? = null
        var maxSdkVersion: Any? = null

        override fun attr(ns: String?, name: String?, resourceId: Int, raw: String?, value: Res_value?) {
            if ("name" == name && value?.type == Res_value.TYPE_STRING) {
                this.name = value.toString()
            } else if ("maxSdkVersion" == name && value?.type == Res_value.TYPE_INT_DEC) {
                this.maxSdkVersion = value.data
            }
            super.attr(ns, name, resourceId, raw, value)
        }

        override fun end() {
            val permName = name
            val maxSdk = maxSdkVersion
            if (permName != null && maxSdk != null) {
                permissionMap[permName] = maxSdk
            }
            super.end()
        }
    }
}