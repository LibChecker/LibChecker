package com.absinthe.libchecker.view.drawable

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.domain.snapshot.timenode.model.DayContribution
import com.absinthe.libchecker.domain.snapshot.timenode.ui.view.SnapshotContributionUpdatesPopup
import com.absinthe.libchecker.ui.preference.model.PreferenceInlineControl
import com.absinthe.libchecker.ui.preference.view.DraggableSegmentedControlView
import com.absinthe.libchecker.view.app.CheckableChipView
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class G2ComponentRenderingInstrumentedTest {
  @Test
  fun chipsAndSegmentedControlsDrawAtCompactAndExpandedWidths() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val chip = CheckableChipView(context).apply {
        text = "G2"
        isChecked = true
      }
      val control = DraggableSegmentedControlView(
        context,
        PreferenceInlineControl.DraggableChoice(listOf("Light", "Dark", "System"), listOf("1", "2", "3"), "2")
      ) {}
      for (view in listOf(chip, control)) {
        for (width in listOf(240, 480)) {
          view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(120, View.MeasureSpec.EXACTLY))
          view.layout(0, 0, width, 120)
          assertDrawsPixels(view)
        }
      }
    }
  }

  @Test
  fun snapshotBalloonsDrawAboveAndBelowTheirAnchor() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      for (nearTop in listOf(true, false)) {
        lateinit var popup: SnapshotContributionUpdatesPopup
        scenario.onActivity { activity ->
          val host = activity.window.decorView
          val y = if (nearTop) 150 else host.height - 150
          popup = SnapshotContributionUpdatesPopup(
            host,
            Rect(host.width / 2 - 10, y, host.width / 2 + 10, y + 20),
            DayContribution(LocalDate.of(2026, 9, 20)),
            emptyMap()
          ) {}
          popup.show()
        }
        try {
          SystemClock.sleep(350)
          scenario.onActivity {
            assertTrue(popup.isShowing)
            assertDrawsPixels(popup.contentView)
          }
        } finally {
          scenario.onActivity { popup.dismissImmediately() }
        }
      }
    }
  }

  private fun assertDrawsPixels(view: View) {
    assertTrue(view.width > 0 && view.height > 0)
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
    val pixels = IntArray(view.width * view.height)
    bitmap.getPixels(pixels, 0, view.width, 0, 0, view.width, view.height)
    assertTrue(pixels.count { it ushr 24 != 0 } > 100)
    bitmap.recycle()
  }
}
