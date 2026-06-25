package com.absinthe.libchecker.features.applist.detail.ui

import android.content.Context
import androidx.annotation.StringRes
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.SIGNATURES
import com.absinthe.libchecker.constant.AbilityType
import com.absinthe.libchecker.domain.app.detail.content.BuildAppDetailTabTypesUseCase

data class DetailTabSpec(
  val types: List<Int>,
  val titles: List<CharSequence>
)

class DetailTabSpecBuilder(
  private val context: Context,
  private val buildAppDetailTabTypes: BuildAppDetailTabTypesUseCase
) {

  fun build(isHarmonyMode: Boolean, isApkPreview: Boolean): DetailTabSpec {
    val types = buildAppDetailTabTypes(
      isHarmonyMode = isHarmonyMode,
      isApkPreview = isApkPreview
    )

    return DetailTabSpec(
      types = types,
      titles = types.map { context.getText(titleResOf(it, isHarmonyMode)) }
    )
  }

  @StringRes
  private fun titleResOf(type: Int, isHarmonyMode: Boolean): Int {
    return if (isHarmonyMode) {
      harmonyTitleResByType[type]
    } else {
      normalTitleResByType[type]
    } ?: R.string.ref_category_native
  }

  private companion object {
    val normalTitleResByType = mapOf(
      NATIVE to R.string.ref_category_native,
      SERVICE to R.string.ref_category_service,
      ACTIVITY to R.string.ref_category_activity,
      RECEIVER to R.string.ref_category_br,
      PROVIDER to R.string.ref_category_cp,
      PERMISSION to R.string.ref_category_perm,
      METADATA to R.string.ref_category_metadata,
      SIGNATURES to R.string.ref_category_signatures
    )

    val harmonyTitleResByType = mapOf(
      NATIVE to R.string.ref_category_native,
      AbilityType.PAGE to R.string.ability_page,
      AbilityType.SERVICE to R.string.ability_service,
      AbilityType.WEB to R.string.ability_web,
      AbilityType.DATA to R.string.ability_data,
      SIGNATURES to R.string.ref_category_signatures
    )
  }
}
