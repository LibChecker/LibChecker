package com.absinthe.libchecker.domain.app.detail.content

import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.SIGNATURES
import com.absinthe.libchecker.constant.AbilityType

class BuildAppDetailTabTypesUseCase {

  operator fun invoke(
    isHarmonyMode: Boolean,
    isApkPreview: Boolean
  ): List<Int> {
    val types = if (isHarmonyMode) {
      harmonyTypes
    } else {
      normalTypes
    }

    return if (isApkPreview) {
      types.filterNot { it == SIGNATURES }
    } else {
      types
    }
  }

  private companion object {
    val normalTypes = listOf(
      NATIVE,
      SERVICE,
      ACTIVITY,
      RECEIVER,
      PROVIDER,
      PERMISSION,
      METADATA,
      SIGNATURES
    )

    val harmonyTypes = listOf(
      NATIVE,
      AbilityType.PAGE,
      AbilityType.SERVICE,
      AbilityType.WEB,
      AbilityType.DATA,
      SIGNATURES
    )
  }
}
