package com.absinthe.libchecker.bridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.services.ACTION_SHOOT_AND_STOP_AUTO
import com.absinthe.libchecker.services.EXTRA_DROP_PREVIOUS
import com.absinthe.libchecker.services.ShootService
import timber.log.Timber

class BridgeActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    intent.data?.let { uri ->
      if (uri.scheme == "lc" && uri.host == "bridge") {
        val action = uri.getQueryParameter("action")

        if (action == "shoot") {
          val authority = uri.getQueryParameter("authority")?.toInt() ?: 0

          if (authority == LibCheckerApp.generateAuthKey()) {
            val dropPrevious = uri.getQueryParameter("drop_previous")?.toBoolean() ?: false

            startService(
              Intent(this, ShootService::class.java).also {
                it.action = ACTION_SHOOT_AND_STOP_AUTO
                it.putExtra(EXTRA_DROP_PREVIOUS, dropPrevious)
              }
            )
          } else {
            Timber.w("Authority mismatch: $authority")
          }
        }
      }
    }
    finish()
  }
}
