package com.absinthe.libchecker.constant.librarymap

import com.absinthe.libchecker.constant.LibChip

/**
 * <pre>
 * author : Absinthe
 * time : 2020/08/08
 * </pre>
 */
object DexLibMap : BaseMap() {

    private val MAP: HashMap<String, LibChip> = hashMapOf()

    override fun getMap(): HashMap<String, LibChip> {
        return MAP
    }

    override fun findRegex(name: String): LibChip? {
        return null
    }
}