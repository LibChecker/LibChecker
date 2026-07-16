package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.domain.app.detail.resource.AppResourcePreview
import com.absinthe.libchecker.domain.app.detail.resource.AppResourceReference

data class LibStringMetadataItemDisplay(
  override val name: LibStringItemNameDisplay,
  val originalDisplayValue: String,
  val resource: AppResourceReference?,
  val preview: AppResourcePreview = AppResourcePreview.Original
) : LibStringItemDisplay {

  val visibleValue: CharSequence = when (preview) {
    is AppResourcePreview.Text -> preview.value
    else -> originalDisplayValue
  }

  override val contentDescription: String = buildLibStringItemDescription(name.text, visibleValue)

  val isTransformed: Boolean = preview != AppResourcePreview.Original

  fun restore(): LibStringMetadataItemDisplay = copy(preview = AppResourcePreview.Original)

  companion object {
    fun create(
      item: LibStringItemChip,
      itemDisplayOptions: Int,
      apkPreviewUnavailableLabel: String,
      preview: AppResourcePreview = AppResourcePreview.Original
    ): LibStringMetadataItemDisplay {
      val isApkPreview = item.item.size == -1L
      return LibStringMetadataItemDisplay(
        name = LibStringItemNameDisplay.create(
          item = item.item,
          type = METADATA,
          itemDisplayOptions = itemDisplayOptions
        ),
        originalDisplayValue = if (isApkPreview) {
          "<$apkPreviewUnavailableLabel>"
        } else {
          item.item.source.orEmpty()
        },
        resource = if (isApkPreview) {
          null
        } else {
          AppResourceReference.create(
            resourceId = item.item.size.toInt(),
            resourceType = item.labels.firstOrNull()
          )
        },
        preview = preview
      )
    }
  }
}
