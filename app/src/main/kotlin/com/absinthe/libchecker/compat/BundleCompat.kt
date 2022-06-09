@file:Suppress("DEPRECATION")

package com.absinthe.libchecker.compat

import android.os.Bundle
import android.os.Parcelable
import com.absinthe.libchecker.utils.OsUtils
import java.io.Serializable

object BundleCompat {
  inline fun <reified T : Parcelable> getParcelable(bundle: Bundle, key: String?): T? {
    return if (OsUtils.atLeastT()) {
      bundle.getParcelable(key, T::class.java)
    } else {
      bundle.getParcelable(key)
    }
  }

  inline fun <reified T : Serializable> getSerializable(bundle: Bundle, key: String?): T? {
    return if (OsUtils.atLeastT()) {
      bundle.getSerializable(key, T::class.java)
    } else {
      bundle.getSerializable(key) as T?
    }
  }
}
