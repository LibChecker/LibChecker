package com.absinthe.libchecker.utils

import android.content.Context
import androidx.core.content.edit
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.constant.SP_NAME

object SPUtils {

    private val sp by lazy {
        LibCheckerApp.context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }

    fun putString(key: String, value: String) {
        sp.edit { putString(key, value) }
    }

    fun getString(key: String): String {
        return sp.getString(key, "") ?: ""
    }

    fun getString(key: String, defaultValue: String): String {
        return sp.getString(key, defaultValue) ?: ""
    }

    fun putBoolean(key: String, value: Boolean) {
        sp.edit { putBoolean(key, value) }
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sp.getBoolean(key, defaultValue)
    }

    fun putInt(key: String, value: Int) {
        sp.edit { putInt(key, value) }
    }

    fun getInt(key: String): Int {
        return sp.getInt(key, 0)
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return sp.getInt(key, defaultValue)
    }

    fun putLong(key: String, value: Long) {
        sp.edit { putLong(key, value) }
    }

    fun getLong(key: String): Long {
        return sp.getLong(key, 0)
    }

    fun getLong(key: String, defaultValue: Long): Long {
        return sp.getLong(key, defaultValue)
    }
}