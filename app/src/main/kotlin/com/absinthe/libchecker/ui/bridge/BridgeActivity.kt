package com.absinthe.libchecker.ui.bridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.LCUris
import com.absinthe.libchecker.services.ACTION_SHOOT_AND_STOP_AUTO
import com.absinthe.libchecker.services.EXTRA_DROP_PREVIOUS
import com.absinthe.libchecker.services.ShootService
import timber.log.Timber

class BridgeActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    intent.data?.let { uri ->
      if (uri.scheme != LCUris.SCHEME || uri.host != LCUris.Bridge.AUTHORITY) {
        return@let
      }
      val action = uri.getQueryParameter(LCUris.Bridge.PARAM_ACTION)

      if (action == LCUris.Bridge.ACTION_SHOOT) {
        val authority = uri.getQueryParameter(LCUris.Bridge.PARAM_AUTHORITY)?.toInt() ?: 0

        if (authority == GlobalValues.generateAuthKey()) {
          val dropPrevious = uri.getQueryParameter(LCUris.Bridge.PARAM_DROP_PREVIOUS)?.toBoolean() == true

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
    finish()
  }
}
