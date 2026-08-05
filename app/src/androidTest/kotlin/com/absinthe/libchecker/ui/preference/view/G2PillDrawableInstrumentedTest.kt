package com.absinthe.libchecker.ui.preference.view

import android.graphics.Color
import android.graphics.Outline
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class G2PillDrawableInstrumentedTest {

  @Test
  fun getOutlineSupportsAndroidSeven() {
    val drawable = G2PillDrawable(fillColor = Color.BLACK)
    drawable.setBounds(0, 0, 240, 48)
    val outline = Outline()

    drawable.getOutline(outline)

    assertTrue(outline.canClip())
  }
}
