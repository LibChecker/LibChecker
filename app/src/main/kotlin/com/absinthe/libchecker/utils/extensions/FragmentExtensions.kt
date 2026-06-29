package com.absinthe.libchecker.utils.extensions

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.MainThread
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import java.io.Serializable

@MainThread
fun <T : Fragment> T.putArguments(bundle: Bundle): T {
  arguments = bundle
  return this
}

@MainThread
fun <T : Fragment> T.putArguments(vararg pairs: Pair<String, Any?>): T = putArguments(
  Bundle(pairs.size).apply {
    pairs.forEach { (key, value) ->
      putValue(key, value)
    }
  }
)

private fun Bundle.putValue(key: String, value: Any?) {
  when (value) {
    null -> putString(key, null)
    is String -> putString(key, value)
    is CharSequence -> putCharSequence(key, value)
    is Int -> putInt(key, value)
    is Boolean -> putBoolean(key, value)
    is Long -> putLong(key, value)
    is Short -> putShort(key, value)
    is Float -> putFloat(key, value)
    is Double -> putDouble(key, value)
    is Bundle -> putBundle(key, value)
    is Parcelable -> putParcelable(key, value)
    is ArrayList<*> -> putArrayList(key, value)
    is Serializable -> putSerializable(key, value)
    else -> throw IllegalArgumentException("Unsupported bundle value type ${value::class.java.name} for key $key")
  }
}

@Suppress("UNCHECKED_CAST")
private fun Bundle.putArrayList(key: String, value: ArrayList<*>) {
  when {
    value.isEmpty() -> putParcelableArrayList(key, ArrayList<Parcelable>())
    value.all { it is Parcelable } -> putParcelableArrayList(key, value as ArrayList<Parcelable>)
    value.all { it is String } -> putStringArrayList(key, value as ArrayList<String>)
    value.all { it is CharSequence } -> putCharSequenceArrayList(key, value as ArrayList<CharSequence>)
    else -> putSerializable(key, value)
  }
}

fun DialogFragment.isShowing() = this.dialog?.isShowing == true && !this.isRemoving
