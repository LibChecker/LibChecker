@file:Suppress("DEPRECATION")

package com.absinthe.libchecker.compat

import android.content.Intent
import android.os.Parcelable
import androidx.core.content.IntentCompat as AndroidXIntentCompat
import java.io.Serializable

object IntentCompat {
  inline fun <reified T : Parcelable> getParcelableExtra(intent: Intent, name: String): T? {
    return AndroidXIntentCompat.getParcelableExtra(intent, name, T::class.java)
  }

  inline fun <reified T : Serializable> getSerializableExtra(intent: Intent, name: String): T? {
    return AndroidXIntentCompat.getSerializableExtra(intent, name, T::class.java)
  }
}
