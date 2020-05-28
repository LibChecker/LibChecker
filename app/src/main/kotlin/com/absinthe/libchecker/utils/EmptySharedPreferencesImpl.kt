package com.absinthe.libchecker.utils

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import java.util.*


class EmptySharedPreferencesImpl : SharedPreferences {

    override fun getAll(): Map<String, *> {
        return HashMap<String, Any>()
    }

    override fun getString(key: String, defValue: String?): String? {
        return defValue
    }

    override fun getStringSet(
        key: String,
        defValues: Set<String>?
    ): Set<String>? {
        return defValues
    }

    override fun getInt(key: String, defValue: Int): Int {
        return defValue
    }

    override fun getLong(key: String, defValue: Long): Long {
        return defValue
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return defValue
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return defValue
    }

    override fun contains(key: String): Boolean {
        return false
    }

    override fun edit(): SharedPreferences.Editor {
        return EditorImpl()
    }

    override fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener) {}

    private class EditorImpl : SharedPreferences.Editor {

        override fun putString(
            key: String,
            value: String?
        ): SharedPreferences.Editor {
            return this
        }

        override fun putStringSet(
            key: String,
            values: Set<String>?
        ): SharedPreferences.Editor {
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            return this
        }

        override fun putLong(
            key: String,
            value: Long
        ): SharedPreferences.Editor {
            return this
        }

        override fun putFloat(
            key: String,
            value: Float
        ): SharedPreferences.Editor {
            return this
        }

        override fun putBoolean(
            key: String,
            value: Boolean
        ): SharedPreferences.Editor {
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            return this
        }

        override fun commit(): Boolean {
            return true
        }

        override fun apply() {}
    }
}