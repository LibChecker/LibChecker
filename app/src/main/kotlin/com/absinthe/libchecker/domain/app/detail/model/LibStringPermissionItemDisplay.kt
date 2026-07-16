package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.annotation.PERMISSION

data class LibStringPermissionItemDisplay(
  override val name: LibStringItemNameDisplay,
  val showNotGrantedIndicator: Boolean,
  val notGrantedLabel: String?,
  override val contentDescription: String
) : LibStringItemDisplay {

  companion object {
    fun create(
      item: LibStringItemChip,
      itemDisplayOptions: Int,
      notGrantedLabel: String
    ): LibStringPermissionItemDisplay {
      val showNotGrantedIndicator = item.item.size == 0L
      val visibleNotGrantedLabel = notGrantedLabel.takeIf { showNotGrantedIndicator }
      val name = LibStringItemNameDisplay.create(
        item = item.item,
        type = PERMISSION,
        itemDisplayOptions = itemDisplayOptions
      )

      return LibStringPermissionItemDisplay(
        name = name,
        showNotGrantedIndicator = showNotGrantedIndicator,
        notGrantedLabel = visibleNotGrantedLabel,
        contentDescription = buildLibStringItemDescription(
          name.text,
          visibleNotGrantedLabel
        )
      )
    }
  }
}
