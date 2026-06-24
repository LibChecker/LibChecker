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

data class DetailTabSpec(
  val types: List<Int>,
  val titles: List<CharSequence>
)

class DetailTabSpecBuilder(private val context: Context) {

  fun build(isHarmonyMode: Boolean, isApkPreview: Boolean): DetailTabSpec {
    val items = if (!isHarmonyMode) {
      normalItems
    } else {
      harmonyItems
    }
    val visibleItems = if (isApkPreview) {
      items.filterNot { it.type == SIGNATURES }
    } else {
      items
    }

    return DetailTabSpec(
      types = visibleItems.map { it.type },
      titles = visibleItems.map { context.getText(it.titleRes) }
    )
  }

  private data class Item(
    val type: Int,
    @StringRes val titleRes: Int
  )

  private companion object {
    val normalItems = listOf(
      Item(NATIVE, R.string.ref_category_native),
      Item(SERVICE, R.string.ref_category_service),
      Item(ACTIVITY, R.string.ref_category_activity),
      Item(RECEIVER, R.string.ref_category_br),
      Item(PROVIDER, R.string.ref_category_cp),
      Item(PERMISSION, R.string.ref_category_perm),
      Item(METADATA, R.string.ref_category_metadata),
      Item(SIGNATURES, R.string.ref_category_signatures)
    )

    val harmonyItems = listOf(
      Item(NATIVE, R.string.ref_category_native),
      Item(AbilityType.PAGE, R.string.ability_page),
      Item(AbilityType.SERVICE, R.string.ability_service),
      Item(AbilityType.WEB, R.string.ability_web),
      Item(AbilityType.DATA, R.string.ability_data),
      Item(SIGNATURES, R.string.ref_category_signatures)
    )
  }
}
