package com.absinthe.libchecker.constant.librarymap

import com.absinthe.libchecker.constant.LibChip

abstract class BaseMap {
    abstract fun getMap(): HashMap<String, LibChip>
}