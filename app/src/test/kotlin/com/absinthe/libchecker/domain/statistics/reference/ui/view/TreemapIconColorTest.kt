package com.absinthe.libchecker.domain.statistics.reference.ui.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TreemapIconColorTest {
  @Test
  fun dominantColorIgnoresTransparentPixelsAndRetainsOpaqueAccent() {
    val red = 0xffff0000.toInt()
    val blue = 0xff0000ff.toInt()
    val pixels = IntArray(60) { red } + IntArray(30) { blue } + IntArray(200) { 0x00ffffff }
    assertEquals(red, TreemapIconColor.dominantColor(pixels))
    assertEquals(red, TreemapIconColor.dominantColor(pixels + IntArray(100) { 0x7fffffff }))
    assertEquals(red, TreemapIconColor.dominantColor(pixels.reversedArray()))
  }

  @Test
  fun monochromeAndEmptyArtworkUsesSurfaceInsteadOfAccent() {
    assertNull(TreemapIconColor.dominantColor(intArrayOf()))
    assertNull(TreemapIconColor.dominantColor(IntArray(100) { 0x00ffffff }))
    assertNull(TreemapIconColor.dominantColor(IntArray(100) { 0xffff0000.toInt() }))
    assertNull(TreemapIconColor.dominantColor(intArrayOf(0xff111111.toInt(), 0xff888888.toInt(), 0xffeeeeee.toInt())))
  }
}
