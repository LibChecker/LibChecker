package com.absinthe.libchecker.domain.statistics.reference.model

import android.content.pm.PackageInfo
import androidx.annotation.DrawableRes
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.isComponentType

data class LibReferenceItemDisplay(
  val label: String,
  val italicLabel: Boolean,
  val libName: String,
  val count: String,
  @DrawableRes val iconRes: Int,
  val iconContentDescription: String,
  val desaturateIcon: Boolean,
  val canOpenDetail: Boolean,
  val contentDescription: String
) {

  companion object {
    fun create(
      reference: LibReference,
      colorfulRuleIcon: Boolean,
      notMarkedLabel: String,
      countText: String
    ): LibReferenceItemDisplay {
      val rule = reference.rule
      val label = rule?.label ?: notMarkedLabel
      val isAndroidGroupPermission = reference.type == PERMISSION &&
        reference.libName.startsWith("android.permission")
      val isAndroidGroupAction = reference.type == ACTION &&
        reference.libName.startsWith("android.intent.action")
      val iconRes = rule?.iconRes ?: if (isAndroidGroupPermission || isAndroidGroupAction) {
        com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android
      } else {
        R.drawable.ic_question
      }

      return LibReferenceItemDisplay(
        label = label,
        italicLabel = rule == null,
        libName = reference.libName,
        count = countText,
        iconRes = iconRes,
        iconContentDescription = rule?.label ?: reference.libName,
        desaturateIcon = rule != null && !colorfulRuleIcon && !rule.isSimpleColorIcon,
        canOpenDetail = reference.canOpenDetail(),
        contentDescription = buildReferenceItemDescription(label, reference.libName, countText)
      )
    }
  }
}

data class MultipleAppsIconItemDisplay(
  val iconPackages: List<PackageInfo>,
  val label: String,
  val libName: String,
  val count: String,
  val contentDescription: String
) {

  companion object {
    fun create(
      reference: LibReference,
      notMarkedLabel: String
    ): MultipleAppsIconItemDisplay {
      val libName = if (reference.type == PACKAGE) {
        reference.libName + ".*"
      } else {
        reference.libName
      }
      val count = reference.referredList.size.toString()

      return MultipleAppsIconItemDisplay(
        iconPackages = reference.iconPackages,
        label = notMarkedLabel,
        libName = libName,
        count = count,
        contentDescription = buildReferenceItemDescription(notMarkedLabel, libName, count)
      )
    }
  }
}

fun LibReference.canOpenDetail(): Boolean {
  return type == NATIVE || isComponentType(type) || type == ACTION
}

private fun buildReferenceItemDescription(vararg parts: String): String {
  return parts
    .map(String::trim)
    .filter(String::isNotEmpty)
    .joinToString()
}
