package com.absinthe.libchecker.domain.app.detail.ui.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveIconLayerCardGeometryTest {

  @Test
  fun `stitch tail is a parallel inset of the asymmetric outer tail`() {
    val bodyLeft = 18f
    val tipX = 0f
    val centerY = 30f
    val topHeight = 6f
    val bottomHeight = 17f
    val inset = 5.5f
    val verticalOffset = 2f

    val stitchTail = calculateInsetTailPlacement(
      bodyLeft = bodyLeft,
      tipX = tipX,
      centerY = centerY,
      topHeight = topHeight,
      bottomHeight = bottomHeight,
      inset = inset,
      verticalOffset = verticalOffset
    )

    assertEquals(inset, stitchTail.bodyLeft - bodyLeft, 0f)
    assertEquals(inset, stitchTail.tipX - tipX, 0f)
    assertEquals(bodyLeft - tipX, stitchTail.bodyLeft - stitchTail.tipX, 0f)
    assertEquals(verticalOffset, stitchTail.centerY - centerY, 0f)
    assertEquals(topHeight, stitchTail.topHeight, 0f)
    assertEquals(bottomHeight, stitchTail.bottomHeight, 0f)
  }

  @Test
  fun `particle disintegration staggers from left to right and fully disappears`() {
    val leftParticle = calculateParticleLocalProgress(
      progress = 0.4f,
      normalizedX = 0.1f,
      activationJitter = 0f
    )
    val rightParticle = calculateParticleLocalProgress(
      progress = 0.4f,
      normalizedX = 0.9f,
      activationJitter = 0f
    )

    assertTrue(leftParticle > rightParticle)
    assertEquals(0f, calculateParticleLocalProgress(0f, 0.5f, 0f), 0f)
    val finishedParticle = calculateParticleLocalProgress(1f, 0.5f, 0f)
    assertEquals(1f, finishedParticle, 0f)
    assertEquals(0f, calculateParticleAlpha(finishedParticle), 0f)
  }
}
