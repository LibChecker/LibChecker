package com.absinthe.libchecker.utils.manifest

import android.util.ArrayMap
import java.io.File
import pxb.android.axml.NodeVisitor

class ApplicationReader(apk: File) : AbstractReader(apk) {

    private val properties: ArrayMap<String, Any> = ArrayMap()

    private inner class ManifestTagVisitor(child: NodeVisitor?) : NodeVisitor(child) {
        private var name: String? = null
        private var value: Any? = null

        override fun child(ns: String?, name: String?): NodeVisitor? {
            val child = super.child(ns, name)
            if ("application" == name) {
                return ApplicationTagVisitor(child)
            }
            return child
        }

        override fun attr(ns: String, name: String, resourceId: Int, type: Int, obj: Any) {
            this.name = name
            this.value = obj

            if (name != null && value != null) {
                properties[name] = value
            }
            super.attr(ns, name, resourceId, type, obj)
        }

        override fun end() {
            if (name != null && value != null) {
                properties[name] = value;
            }
            super.end()
        }

        private inner class ApplicationTagVisitor(child: NodeVisitor?) : NodeVisitor(child) {
            private var name: String? = null
            private var value: Any? = null

            override fun attr(ns: String, name: String, resourceId: Int, type: Int, obj: Any) {
                this.name = name
                this.value = obj

                if (name != null && value != null) {
                    properties[name] = value
                }
                super.attr(ns, name, resourceId, type, obj)
            }

            override fun end() {
                if (name != null && value != null) {
                    properties[name] = value;
                }
                super.end()
            }
        }
    }

    companion object {
        fun getManifestProperties(apk: File): Map<String, Any> {
            return ApplicationReader(apk).properties
        }
    }

    override fun provideNodeVisitor(child: NodeVisitor?): NodeVisitor {
        return ManifestTagVisitor(child)
    }
}


