package com.absinthe.libchecker.domain.app

import com.absinthe.libchecker.utils.apk.ApkPreview
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetApkPreviewInfoUseCase {

  suspend operator fun invoke(url: String): Result<ApkPreviewInfo> = withContext(Dispatchers.IO) {
    ApkPreview(url).parse()
  }
}
