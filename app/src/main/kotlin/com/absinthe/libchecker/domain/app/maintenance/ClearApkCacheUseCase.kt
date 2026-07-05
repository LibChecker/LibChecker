package com.absinthe.libchecker.domain.app.maintenance

import android.content.Context
import com.absinthe.libchecker.utils.extensions.requireAvailableCacheDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClearApkCacheUseCase(
  private val context: Context
) {

  suspend operator fun invoke() = withContext(Dispatchers.IO) {
    context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
    context.requireAvailableCacheDir()
  }
}
