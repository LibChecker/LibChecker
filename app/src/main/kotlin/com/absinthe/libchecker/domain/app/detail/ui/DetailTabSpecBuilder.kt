package com.absinthe.libchecker.domain.app.detail.ui

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
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.constant.AbilityType

data class DetailTabItem(
  val type: Int,
  val title: CharSequence
)

data class DetailTabSpec(
  val items: List<DetailTabItem> = emptyList()
) {
  val types: List<Int>
    get() = items.map(DetailTabItem::type)

  fun itemAt(position: Int): DetailTabItem? {
    return items.getOrNull(position)
  }

  fun withStaticLibraryTab(title: CharSequence): DetailTabSpec {
    if (items.any { it.type == STATIC }) {
      return this
    }
    val insertionIndex = STATIC_LIBRARY_POSITION.coerceAtMost(items.size)
    return copy(
      items = items.toMutableList().apply {
        add(insertionIndex, DetailTabItem(STATIC, title))
      }
    )
  }

  companion object {
    const val STATIC_LIBRARY_POSITION = 1
  }
}

class DetailTabSpecBuilder(private val context: Context) {

  fun build(isHarmonyMode: Boolean, isApkPreview: Boolean): DetailTabSpec {
    val types = (if (isHarmonyMode) harmonyTypes else normalTypes).let {
      if (isApkPreview) it.filterNot { type -> type == SIGNATURES } else it
    }

    return DetailTabSpec(
      items = types.map { type ->
        DetailTabItem(
          type = type,
          title = context.getText(titleResOf(type, isHarmonyMode))
        )
      }
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
