package com.absinthe.libchecker.utils.manifest

import android.util.ArrayMap
import java.io.File
import pxb.android.axml.NodeVisitor

class ManifestReader(apk: File, private val demands: Array<String>?): AbstractReader(apk) {

    private val properties = ArrayMap<String, Any>()


    override fun provideNodeVisitor(child: NodeVisitor?): NodeVisitor {
        return ManifestTagVisitor(child)
    }


    companion object {
        fun getManifestProperties(apk: File, demands: Array<String>): Map<String, Any> {
            return ManifestReader(apk, demands).properties
        }
    }

    private fun contains(name: String): Boolean {
        demands?.forEach {
            if (it == name) return true
        }
        return false
    }

    private inner class ManifestTagVisitor(child: NodeVisitor?): NodeVisitor(child) {
        private var name: String? = null
        private var value: Any? = null

        override fun child(ns: String?, name: String): NodeVisitor? {
            val child = super.child(ns, name)
            when (name) {
                "application" -> return ApplicationTagVisitor(child)
                "uses-sdk" -> return UsesSdkTagVisitor(child)
                "overlay" -> {
                    properties["overlay"] = true
                    return OverlayTagVisitor(child)
                }

                else -> {}
            }
            return child
        }

        override fun attr(ns: String?, name: String, resourceId: Int, type: Int, obj: Any) {
            if (contains(name)) {
                this.name = name
                this.value = obj

                if (this.name != null && value != null) {
                  properties[name] = value
                }
            }
            super.attr(ns, name, resourceId, type, obj)
        }

        override fun end() {
            if (name != null && value != null) {
              properties[name] = value
            }
            super.end()
        }

        private inner class ApplicationTagVisitor(child: NodeVisitor?) : NodeVisitor(child) {
            private var name: String? = null
            private var value: Any? = null

            override fun attr(ns: String?, name: String, resourceId: Int, type: Int, obj: Any) {
                if (contains(name)) {
                    this.name = name
                    this.value = obj

                    if (this.name != null && value != null) {
                        properties[name] = value
                    }
                }
                super.attr(ns, name, resourceId, type, obj)
            }

            override fun end() {
                if (name != null && value != null) {
                    properties[name] = value
                }
                super.end()
            }
        }

        private inner class UsesSdkTagVisitor(child: NodeVisitor?): NodeVisitor(child) {
            private var name: String? = null
            private var value: Any? = null

            override fun attr(ns: String?, name: String, resourceId: Int, type: Int, obj: Any) {
                if (contains(name)) {
                    this.name = name
                    value = obj
                }
                super.attr(ns, name, resourceId, type, obj)
            }

            override fun end() {
                if (name != null && value != null) {
                    properties[name] = value
                }
                super.end()
            }
        }

        private inner class OverlayTagVisitor(child: NodeVisitor?) : NodeVisitor(child) {
            private var name: String? = null
            private var value: Any? = null

            override fun attr(ns: String?, name: String, resourceId: Int, type: Int, obj: Any) {
                if (contains(name)) {
                    this.name = name
                    value = obj
                }
                super.attr(ns, name, resourceId, type, obj)
            }

            override fun end() {
                if (name != null && value != null) {
                    properties[name] = value
                }
                super.end()
            }
        }
    }
}
