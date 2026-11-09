package com.absinthe.libchecker.domain.statistics.reference.ui

import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.widget.Toolbar
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.dp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibReferenceActionBarIconInstrumentedTest {
  @Test
  fun oversizedIconsAreBoundedWithoutStretchingOrUpscaling() {
    for ((width, height) in listOf(1024 to 1024, 1024 to 256, 256 to 1024, 12 to 6)) {
      val icon = GradientDrawable().apply { setSize(width, height) }.boundedReferenceIcon(24)
      assertTrue(icon.intrinsicWidth in 1..24)
      assertTrue(icon.intrinsicHeight in 1..24)
      assertEquals(width.toFloat() / height, icon.intrinsicWidth.toFloat() / icon.intrinsicHeight, 0.001f)
      assertEquals(minOf(24, maxOf(width, height)), maxOf(icon.intrinsicWidth, icon.intrinsicHeight))
    }
  }

  @Test
  fun actionBarShowsReferenceIcon() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val rule = requireNotNull(runBlocking { RulesRepository.getRule("libAGFX.so", NATIVE, true) })
    assertNotEquals(R.drawable.ic_question, rule.iconRes)
    val expectedIcon = requireNotNull(context.getDrawable(rule.iconRes)).boundedReferenceIcon(24.dp)
    val intent = Intent(context, LibReferenceActivity::class.java)
      .putExtra(EXTRA_REF_NAME, "libAGFX.so")
      .putExtra(EXTRA_REF_LABEL, "BytePlus RTC")
      .putExtra(EXTRA_REF_TYPE, NATIVE)

    ActivityScenario.launch<LibReferenceActivity>(intent).use { scenario ->
      val deadline = System.nanoTime() + 5_000_000_000L
      while (System.nanoTime() < deadline) {
        var icon: Drawable? = null
        scenario.onActivity { icon = it.findViewById<Toolbar>(R.id.toolbar).logo }
        val currentIcon = icon
        if (currentIcon != null) {
          assertTrue(UiUtils.drawablesAreEqual(expectedIcon, currentIcon))
          return@use
        }
        Thread.sleep(25)
      }
      scenario.onActivity {
        assertTrue(UiUtils.drawablesAreEqual(expectedIcon, it.findViewById<Toolbar>(R.id.toolbar).logo))
      }
    }
  }
}
