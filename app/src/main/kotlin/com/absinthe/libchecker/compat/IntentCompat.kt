@file:Suppress("DEPRECATION")

package com.absinthe.libchecker.compat

import android.content.Intent
import android.os.Parcelable
import com.absinthe.libchecker.utils.OsUtils
import java.io.Serializable

object IntentCompat {
  inline fun <reified T : Parcelable> getParcelableExtra(intent: Intent, name: String): T? {
    return if (OsUtils.atLeastT()) {
      intent.getParcelableExtra(name, T::class.java)
    } else {
      intent.getParcelableExtra(name)
    }
  }

  inline fun <reified T : Serializable> getSerializableExtra(intent: Intent, name: String): T? {
    return if (OsUtils.atLeastT()) {
      intent.getSerializableExtra(name, T::class.java)
    } else {
      intent.getSerializableExtra(name) as T?
    }
  }
}
