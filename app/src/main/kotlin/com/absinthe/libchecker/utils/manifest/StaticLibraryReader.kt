package com.absinthe.libchecker.utils.manifest

import android.util.ArrayMap
import java.io.File
import pxb.android.axml.NodeVisitor

class StaticLibraryReader(apk: File) : AbstractReader(apk) {

    private val staticLibs = ArrayMap<String, Any>()

    override fun provideNodeVisitor(child: NodeVisitor?): NodeVisitor {
        return ManifestTagVisitor(child)
    }


    private inner class ManifestTagVisitor(child: NodeVisitor?): NodeVisitor(child) {
        override fun child(ns: String?, name: String): NodeVisitor? {
            val child = super.child(ns, name)
            return if ("application" == name) {
                ApplicationTagVisitor(child)
            } else {
              child
            }
        }

        private inner class ApplicationTagVisitor(child: NodeVisitor?): NodeVisitor(child) {
            override fun child(ns: String?, name: String): NodeVisitor? {
                val child = super.child(ns, name)

                return if ("uses-static-library" == name) {
                    StaticLibraryVisitor(child)
                } else {
                    child
                }
            }
        }
    }

    private inner class StaticLibraryVisitor(child: NodeVisitor?): NodeVisitor(child) {
        private var name: String? = null
        private var version: Any? = null

        override fun attr(ns: String?, name: String, resourceId: Int, type: Int, obj: Any) {

           if (type == 3 && "name" == name) {
              this.name = obj as String
           }

           if ("version" == name) {
              version = obj
           }

           super.attr(ns, name, resourceId, type, obj)
        }

        override fun end() {
            if (name != null && version != null) {
              staticLibs.put(name, version)
            }
            super.end()
        }
    }

    companion object {
        fun getStaticLibrary(apk: File): Map<String,Any> {
            return StaticLibraryReader(apk).staticLibs
        }

        fun extractIntPart(str: String): Int {
            var result = 0
            val length: Int = str.length

            for (offset in 0 until length) {
                  val c: Char = str.get(offset)
                  result = if (c in '0'..'9') result * 10 + (c.code - '0'.code) else break
            }

            return result
        }
    }
}
