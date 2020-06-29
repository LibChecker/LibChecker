package com.absinthe.libchecker.constant.librarymap

import com.absinthe.libchecker.constant.LibChip
import com.absinthe.libchecker.recyclerview.adapter.LibStringAdapter
import com.absinthe.libchecker.ui.main.LibReferenceActivity

abstract class BaseMap {

    abstract fun getMap(): HashMap<String, LibChip>
    abstract fun findRegex(name: String): LibChip?

    fun contains(name: String): Boolean {
        return getMap().containsKey(name) || findRegex(name) != null
    }

    fun getChip(name: String): LibChip? {
        getMap()[name]?.let {
            return it
        } ?: let {
            return findRegex(name)
        }
    }

    companion object {
        fun getMap(mode: LibStringAdapter.Mode): BaseMap {
            return when (mode) {
                LibStringAdapter.Mode.NATIVE -> NativeLibMap
                LibStringAdapter.Mode.SERVICE -> ServiceLibMap
                LibStringAdapter.Mode.ACTIVITY -> ActivityLibMap
                LibStringAdapter.Mode.RECEIVER -> ReceiverLibMap
                else -> ProviderLibMap
            }
        }

        fun getMap(type: LibReferenceActivity.Type): BaseMap {
            return when (type) {
                LibReferenceActivity.Type.TYPE_NATIVE -> NativeLibMap
                LibReferenceActivity.Type.TYPE_SERVICE -> ServiceLibMap
                LibReferenceActivity.Type.TYPE_ACTIVITY -> ActivityLibMap
                LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER -> ReceiverLibMap
                else -> ProviderLibMap
            }
        }
    }
}