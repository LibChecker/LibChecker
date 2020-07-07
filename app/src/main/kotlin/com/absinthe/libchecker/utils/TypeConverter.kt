package com.absinthe.libchecker.utils

import com.absinthe.libchecker.recyclerview.adapter.LibStringAdapter
import com.absinthe.libchecker.ui.main.LibReferenceActivity

object TypeConverter {

    fun libRefTypeToMode(type: LibReferenceActivity.Type): LibStringAdapter.Mode {
        return when (type) {
            LibReferenceActivity.Type.TYPE_SERVICE -> LibStringAdapter.Mode.SERVICE
            LibReferenceActivity.Type.TYPE_ACTIVITY -> LibStringAdapter.Mode.ACTIVITY
            LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER -> LibStringAdapter.Mode.RECEIVER
            LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER -> LibStringAdapter.Mode.PROVIDER
            else -> LibStringAdapter.Mode.NATIVE
        }
    }

    fun libModeToRefType(mode: LibStringAdapter.Mode): LibReferenceActivity.Type {
        return when (mode) {
            LibStringAdapter.Mode.SERVICE -> LibReferenceActivity.Type.TYPE_SERVICE
            LibStringAdapter.Mode.ACTIVITY -> LibReferenceActivity.Type.TYPE_ACTIVITY
            LibStringAdapter.Mode.RECEIVER -> LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER
            LibStringAdapter.Mode.PROVIDER -> LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER
            LibStringAdapter.Mode.NATIVE -> LibReferenceActivity.Type.TYPE_NATIVE
        }
    }
}