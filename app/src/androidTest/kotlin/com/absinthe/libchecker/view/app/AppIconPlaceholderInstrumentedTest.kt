package com.absinthe.libchecker.view.app

import android.graphics.drawable.BitmapDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppIconPlaceholderInstrumentedTest {

  @Test
  fun placeholderUsesAppIconLoaderRendering() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val drawable = AppIconPlaceholder.newDrawable(context)

    assertTrue(drawable is BitmapDrawable)
    val bitmap = (drawable as BitmapDrawable).bitmap
    val expectedSize = context.getDimensionPixelSize(R.dimen.app_icon_size)
    assertEquals(expectedSize, bitmap.width)
    assertEquals(expectedSize, bitmap.height)
  }
}
