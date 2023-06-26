package com.absinthe.libchecker.utils

import android.content.pm.PackageManager
import com.absinthe.libchecker.constant.Constants
import rikka.shizuku.Shizuku
import rikka.sui.Sui

object ShizukuUtils {
  fun checkShizukuStatus(): Status = when {
    !PackageUtils.isAppInstalled(Constants.PackageNames.SHIZUKU) && !Sui.isSui() -> Status.NOT_INSTALLED
    !Shizuku.pingBinder() -> Status.NOT_RUNNING
    Shizuku.getVersion() < 10 -> Status.LOW_VERSION
    Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> Status.NOT_AUTHORIZED
    else -> {
      Status.SUCCESS
    }
  }

  enum class Status {
    SUCCESS, NOT_AUTHORIZED, LOW_VERSION ,NOT_RUNNING, NOT_INSTALLED
  }
}
