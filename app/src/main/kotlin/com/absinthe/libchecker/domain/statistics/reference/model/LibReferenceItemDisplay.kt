package com.absinthe.libchecker.domain.statistics.reference.model

import android.content.pm.PackageInfo
import androidx.annotation.DrawableRes
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.SHARED_UID
import com.absinthe.libchecker.annotation.isComponentType

data class LibReferenceSearchLabels(
  val notMarkedLabel: String = "",
  val permissionFallbackLabel: String = "",
  val metadataLabel: String = "",
  val packageLabel: String = "",
  val sharedUidPrefix: String = "UID"
)

internal fun LibReference.matchesSearchQuery(
  query: String,
  labels: LibReferenceSearchLabels
): Boolean {
  if (libName.contains(query, ignoreCase = true)) {
    return true
  }
  val displayLabel = rule?.label ?: when (type) {
    PERMISSION ->
      resolvedLabel
        ?.takeUnless { it == libName }
        ?: labels.permissionFallbackLabel

    METADATA -> labels.metadataLabel

    PACKAGE -> labels.packageLabel

    SHARED_UID -> {
      val uid = iconPackages.firstNotNullOfOrNull { it.applicationInfo?.uid }
      uid?.let { "${labels.sharedUidPrefix} $it" } ?: labels.sharedUidPrefix
    }

    else -> labels.notMarkedLabel
  }
  return displayLabel.contains(query, ignoreCase = true)
}

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
      permissionFallbackLabel: String,
      metadataLabel: String,
      countText: String
    ): LibReferenceItemDisplay {
      val rule = reference.rule
      val categoryLabel = when (reference.type) {
        PERMISSION ->
          reference.resolvedLabel
            ?.takeUnless { it == reference.libName }
            ?: permissionFallbackLabel

        METADATA -> metadataLabel

        else -> null
      }
      val label = rule?.label ?: categoryLabel ?: notMarkedLabel
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
        italicLabel = rule == null && categoryLabel == null,
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
  val italicLabel: Boolean,
  val libName: String,
  val count: String,
  val contentDescription: String
) {

  companion object {
    fun create(
      reference: LibReference,
      notMarkedLabel: String,
      packageLabel: String,
      sharedUidLabel: String
    ): MultipleAppsIconItemDisplay {
      val categoryLabel = when (reference.type) {
        PACKAGE -> packageLabel
        SHARED_UID -> sharedUidLabel
        else -> null
      }
      val libName = when (reference.type) {
        PACKAGE -> reference.libName + ".*"
        else -> reference.libName
      }
      val count = reference.referredList.size.toString()
      val label = categoryLabel ?: notMarkedLabel

      return MultipleAppsIconItemDisplay(
        iconPackages = reference.iconPackages,
        label = label,
        italicLabel = categoryLabel == null,
        libName = libName,
        count = count,
        contentDescription = buildReferenceItemDescription(label, libName, count)
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
