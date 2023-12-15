@file:Suppress("DEPRECATION")

package com.absinthe.libchecker.compat

import android.os.Bundle
import com.absinthe.libchecker.utils.OsUtils
import java.io.Serializable

object BundleCompat {

  // TODO: Remove this method until androidx introduced an official one
  inline fun <reified T : Serializable> getSerializable(bundle: Bundle, key: String?): T? {
    return if (OsUtils.atLeastT()) {
      bundle.getSerializable(key, T::class.java)
    } else {
      bundle.getSerializable(key) as T?
    }
  }
}
