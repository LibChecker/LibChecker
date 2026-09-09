package com.absinthe.libchecker.view.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationBlurInstrumentedTest {
  @Test
  fun darkNavigationTintRevealsBackdropOnlyAsNavigationFloats() {
    val surface = 0xFF204060.toInt()
    assertEquals(0xCC000000.toInt(), darkNavigationTintColor(surface, 0f))
    assertEquals(0xAC102030.toInt(), darkNavigationTintColor(surface, 0.5f))
    assertEquals(0x8C204060.toInt(), darkNavigationTintColor(surface, 1f))
    assertEquals(darkNavigationTintColor(surface, 0f), darkNavigationTintColor(surface, -1f))
    assertEquals(darkNavigationTintColor(surface, 1f), darkNavigationTintColor(surface, 2f))
  }
}
