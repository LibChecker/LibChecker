package com.absinthe.libchecker.domain.app.detail.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DetailIconLayerAlignmentTest {

  @Test
  fun `icon center and card preview center share the same row center`() {
    val rowTop = 100f
    val rowHeight = 60
    val iconCenterOffset = 30f
    val cardPreviewCenterOffset = 44f

    val iconTop = calculateAlignedLayerTop(rowTop, rowHeight, iconCenterOffset)
    val cardTop = calculateAlignedLayerTop(rowTop, rowHeight, cardPreviewCenterOffset)

    assertEquals(iconTop + iconCenterOffset, cardTop + cardPreviewCenterOffset, 0f)
  }
}
