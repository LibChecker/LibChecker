package com.absinthe.libchecker.constant.librarymap

import com.absinthe.libchecker.constant.LibChip

abstract class BaseMap {
    abstract fun getMap(): HashMap<String, LibChip>
    abstract fun findRegex(name: String): LibChip?

    fun contains(name: String): Boolean {
        return getMap().containsKey(name) || NativeLibMap.findRegex(name) != null
    }
}