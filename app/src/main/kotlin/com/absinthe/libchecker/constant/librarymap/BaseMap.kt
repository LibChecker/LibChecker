package com.absinthe.libchecker.constant.librarymap

import com.absinthe.libchecker.constant.LibChip

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
}