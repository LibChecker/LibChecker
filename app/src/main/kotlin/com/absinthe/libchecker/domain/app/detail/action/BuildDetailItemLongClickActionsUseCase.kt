package com.absinthe.libchecker.domain.app.detail.action

import androidx.annotation.DrawableRes
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.ACTION_IN_RULES
import com.absinthe.libchecker.annotation.ET_NOT_ELF
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip

class BuildDetailItemLongClickActionsUseCase {

  operator fun invoke(request: DetailItemLongClickActionRequest): DetailItemLongClickActions {
    val item = request.item
    val componentName = if (request.detailType == PERMISSION) {
      item.item.name.substringBefore(" ")
    } else {
      item.item.name
    }
    val fullComponentName = if (componentName.startsWith(".")) {
      request.packageName + componentName
    } else {
      componentName
    }

    return DetailItemLongClickActions(
      packageName = request.packageName,
      componentName = componentName,
      fullComponentName = fullComponentName,
      copyText = buildCopyText(request.detailType, componentName, item),
      elfExtractAvailable = request.detailType == NATIVE && !request.isApkPreview,
      elfInfo = buildElfInfo(request, item),
      reference = buildReference(request, componentName, item),
      integrationsAvailable = !request.isApk && !request.isApkPreview
    )
  }

  private fun buildCopyText(
    @LibType detailType: Int,
    componentName: String,
    item: LibStringItemChip
  ): String {
    return if (detailType == METADATA) {
      componentName + ": " + item.item.source
    } else {
      componentName
    }
  }

  private fun buildElfInfo(
    request: DetailItemLongClickActionRequest,
    item: LibStringItemChip
  ): DetailItemElfInfoAction? {
    if (request.detailType != NATIVE || request.isApkPreview || item.item.elfInfo.elfType == ET_NOT_ELF) {
      return null
    }

    return DetailItemElfInfoAction(
      packageName = request.packageName,
      elfPath = item.item.source.orEmpty(),
      ruleIcon = item.rule?.iconRes ?: com.absinthe.lc.rulesbundle.R.drawable.ic_sdk_placeholder
    )
  }

  private fun buildReference(
    request: DetailItemLongClickActionRequest,
    componentName: String,
    item: LibStringItemChip
  ): DetailItemReferenceAction? {
    if (!request.canReference || componentName.startsWith(".")) {
      return null
    }

    return DetailItemReferenceAction(
      refName = item.rule?.libName ?: componentName,
      label = item.rule?.label,
      type = if (item.rule?.libType == ACTION_IN_RULES) ACTION else request.detailType
    )
  }
}

data class DetailItemLongClickActionRequest(
  val item: LibStringItemChip,
  val packageName: String,
  @LibType val detailType: Int,
  val canReference: Boolean,
  val isApk: Boolean,
  val isApkPreview: Boolean
)

data class DetailItemLongClickActions(
  val packageName: String,
  val componentName: String,
  val fullComponentName: String,
  val copyText: String,
  val elfExtractAvailable: Boolean,
  val elfInfo: DetailItemElfInfoAction?,
  val reference: DetailItemReferenceAction?,
  val integrationsAvailable: Boolean
)

data class DetailItemElfInfoAction(
  val packageName: String,
  val elfPath: String,
  @DrawableRes val ruleIcon: Int
)

data class DetailItemReferenceAction(
  val refName: String,
  val label: String?,
  @LibType val type: Int
)
