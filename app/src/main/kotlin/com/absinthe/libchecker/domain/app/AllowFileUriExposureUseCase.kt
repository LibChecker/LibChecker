package com.absinthe.libchecker.domain.app

import android.os.StrictMode
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

class AllowFileUriExposureUseCase {

  operator fun invoke() {
    if (!attemptedInProcess.compareAndSet(false, true)) {
      return
    }

    runCatching {
      StrictMode::class.java.getDeclaredMethod("disableDeathOnFileUriExposure").invoke(null)
      Timber.i("StrictMode: disableDeathOnFileUriExposure")
    }.onFailure {
      Timber.w("bypass [StrictMode: disableDeathOnFileUriExposure] failed")
    }
  }

  private companion object {
    val attemptedInProcess = AtomicBoolean(false)
  }
}
