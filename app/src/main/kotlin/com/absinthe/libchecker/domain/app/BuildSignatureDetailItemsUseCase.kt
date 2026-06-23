package com.absinthe.libchecker.domain.app

import com.absinthe.libchecker.features.applist.detail.bean.SignatureDetailItem

class BuildSignatureDetailItemsUseCase {

  operator fun invoke(detail: String): List<SignatureDetailItem> {
    return detail.lines().map {
      val values = it.split(":", limit = 2)
      SignatureDetailItem(
        values.getOrNull(0).orEmpty(),
        values.getOrNull(1).orEmpty()
      )
    }
  }
}
