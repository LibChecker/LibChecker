package com.absinthe.libchecker.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import timber.log.Timber

class SPDelegates<T>(private val key: String, private val default: T) : ReadWriteProperty<Any?, T> {
  override fun getValue(thisRef: Any?, property: KProperty<*>): T {
    Timber.d("getValue $key")
    return SPUtils.getValue(key, default)
  }

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    SPUtils.putValue(key, value)
  }
}
