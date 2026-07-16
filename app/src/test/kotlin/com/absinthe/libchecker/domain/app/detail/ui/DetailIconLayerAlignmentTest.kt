package com.absinthe.libchecker.domain.app.detail.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DetailIconLayerAlignmentTest {

  @Test
  fun `operator center and card preview center share the same row center`() {
    val rowTop = 100f
    val rowHeight = 60
    val operatorCenterOffset = 12f
    val cardPreviewCenterOffset = 44f

    val operatorTop = calculateAlignedLayerTop(rowTop, rowHeight, operatorCenterOffset)
    val cardTop = calculateAlignedLayerTop(rowTop, rowHeight, cardPreviewCenterOffset)

    assertEquals(operatorTop + operatorCenterOffset, cardTop + cardPreviewCenterOffset, 0f)
  }
}
