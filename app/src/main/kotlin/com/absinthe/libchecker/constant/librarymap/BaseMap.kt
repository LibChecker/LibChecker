package com.absinthe.libchecker.constant.librarymap

import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.constant.LibChip
import java.util.regex.Pattern

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

    protected fun matchAllPatterns(content: String, vararg patterns: Pattern): Boolean {
        for (pattern in patterns) {
            if (pattern.matcher(content).matches()) { return true }
        }
        return false
    }

    companion object {
        fun getMap(@LibType type: Int): BaseMap {
            return when (type) {
                NATIVE -> NativeLibMap
                SERVICE -> ServiceLibMap
                ACTIVITY -> ActivityLibMap
                RECEIVER -> ReceiverLibMap
                PROVIDER -> ProviderLibMap
                DEX -> DexLibMap
                PERMISSION -> DexLibMap
                else -> throw IllegalArgumentException("Illegal LibType.")
            }
        }
    }
}