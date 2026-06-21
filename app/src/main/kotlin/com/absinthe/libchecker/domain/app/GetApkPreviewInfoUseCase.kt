package com.absinthe.libchecker.domain.app

import com.absinthe.libchecker.utils.apk.ApkPreview
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo

class GetApkPreviewInfoUseCase {

  operator fun invoke(url: String): Result<ApkPreviewInfo> {
    return ApkPreview(url).parse()
  }
}
