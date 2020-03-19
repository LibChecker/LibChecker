package com.absinthe.libchecker.utils

import android.content.Context
import com.absinthe.libchecker.BuildConfig

object SPUtils {
    private val sPName: String
        get() = "${BuildConfig.APPLICATION_ID}_preferences"

    fun putString(
        context: Context,
        key: String?,
        value: String?
    ) {
        val editor =
            context.getSharedPreferences(sPName, Context.MODE_PRIVATE)
                .edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getString(context: Context, key: String?): String? {
        val sp =
            context.getSharedPreferences(sPName, Context.MODE_PRIVATE)
        return sp.getString(key, "")
    }

    fun getString(
        context: Context,
        key: String?,
        defaultValue: String?
    ): String? {
        val sp =
            context.getSharedPreferences(sPName, Context.MODE_PRIVATE)
        return sp.getString(key, defaultValue)
    }

    fun putBoolean(
        context: Context,
        key: String?,
        value: Boolean
    ) {
        val editor =
            context.getSharedPreferences(sPName, Context.MODE_PRIVATE)
                .edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getBoolean(
        context: Context,
        key: String?,
        defaultValue: Boolean
    ): Boolean {
        val sp =
            context.getSharedPreferences(sPName, Context.MODE_PRIVATE)
        return if (defaultValue) {
            sp.getBoolean(key, true)
        } else {
            sp.getBoolean(key, false)
        }
    }

    fun putInt(context: Context, key: String?, value: Int) {
        val editor =
            context.getSharedPreferences(sPName, Context.MODE_PRIVATE)
                .edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun getInt(context: Context, key: String?): Int {
        val sp =
            context.getSharedPreferences(sPName, Context.MODE_PRIVATE)
        return sp.getInt(key, 0)
    }

    fun getInt(context: Context, key: String?, defaultValue: Int): Int {
        val sp =
            context.getSharedPreferences(sPName, Context.MODE_PRIVATE)
        return sp.getInt(key, defaultValue)
    }

    fun putLong(
        context: Context,
        key: String?,
        value: Long
    ) {
        val editor =
            context.getSharedPreferences(sPName, Context.MODE_PRIVATE)
                .edit()
        editor.putLong(key, value)
        editor.apply()
    }

    fun getLong(context: Context, key: String?): Long {
        val sp =
            context.getSharedPreferences(sPName, Context.MODE_PRIVATE)
        return sp.getLong(key, 0)
    }

    fun getLong(
        context: Context,
        key: String?,
        defaultValue: Long
    ): Long {
        val sp =
            context.getSharedPreferences(sPName, Context.MODE_PRIVATE)
        return sp.getLong(key, defaultValue)
    }
}