package com.absinthe.libchecker.utils.manifest

import com.absinthe.libchecker.compat.IZipFile
import com.absinthe.libchecker.compat.ZipFileCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import pxb.android.axml.AxmlReader
import pxb.android.axml.AxmlVisitor
import pxb.android.axml.NodeVisitor
import timber.log.Timber

abstract class AbstractReader(protected val apk: File) {

    init {
        try {
            val zip: IZipFile = ZipFileCompat(apk)
            zip.use {
                val inputStream = it.getInputStream(it.getEntry(MANIFEST)!!)
                val bytes = getBytesFromInputStream(inputStream)
                val reader = AxmlReader(bytes ?: ByteArray(0))

                reader.accept(object : AxmlVisitor() {
                    override fun child(ns: String?, name: String?): NodeVisitor {
                        val child = super.child(ns, name)
                        return provideNodeVisitor(child)
                    }
                })
            }
        } catch (exception: Exception) {
            Timber.e(exception)
        }
    }

    abstract fun provideNodeVisitor(child: NodeVisitor?): NodeVisitor

    protected fun getBytesFromInputStream(inputStream: InputStream): ByteArray {
        ByteArrayOutputStream().use {boss ->
          val b = ByteArray(1024)

          var n: Int

          while (inputStream.read(b).also { n = it } != -1) {
              boss.write(b, 0, n)
          }

          return boss.toByteArray()
        }
    }

    companion object {
        @JvmStatic
        protected val MANIFEST = "AndroidManifest.xml"
    }
}
