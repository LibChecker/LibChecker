package com.absinthe.libchecker.view.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppIconPlaceholderInstrumentedTest {

  @Test
  fun placeholderLoadsResourceDrawable() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val drawable = AppIconPlaceholder.newDrawable(context)

    assertNotNull(drawable)
    assertTrue(drawable!!.intrinsicWidth > 0)
    assertTrue(drawable.intrinsicHeight > 0)
  }
}
