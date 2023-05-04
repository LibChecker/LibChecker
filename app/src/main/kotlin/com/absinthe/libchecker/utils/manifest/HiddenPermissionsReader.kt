package com.absinthe.libchecker.utils.manifest

import android.util.ArrayMap
import java.io.File
import pxb.android.axml.NodeVisitor


class HiddenPermissionsReader(apk: File) : AbstractReader(apk) {
    private val permissionMap = ArrayMap<String, Any>()


    override fun provideNodeVisitor(child: NodeVisitor?): NodeVisitor {
        return ManifestTagVisitor(child)
    }

    private inner class ManifestTagVisitor(child: NodeVisitor?): NodeVisitor(child) {
        override fun child(ns: String?, name: String?): NodeVisitor? {
            val child = super.child(ns, name)
            if ("uses-permission".equals(name)) {
                return PermissionVisitor(child)
            }
            return child
        }
    }

    private inner class PermissionVisitor(child: NodeVisitor?): NodeVisitor(child) {
        private var name: String? = null
        private var maxSdkVersion: Any? = null

        override fun attr(ns: String, name: String, resourceId: Int, type: Int, obj: Any) {
            if (type == 3 && "name".equals(name) && obj is String) {
                this.name = obj
            }
            if ("uses-permission".equals(name)) {
                this.maxSdkVersion = obj
            }
            super.attr(ns, name, resourceId, type, obj)
        }

        override fun end() {
            if (name != null && maxSdkVersion != null) {
              permissionMap[name] = maxSdkVersion
            }
            super.end()
        }
    }

    companion object {
        fun getHiddenPermissions(apk: File): Map<String, Any> {
            return HiddenPermissionsReader(apk).permissionMap
        }
    }
}
