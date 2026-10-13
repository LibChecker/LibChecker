package com.absinthe.libchecker.domain.snapshot.list.ui.view

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotItemIndicatorInstrumentedTest {
  @Test
  fun longVersionChangeWrapsWithoutClippingFollowingContent() {
    ActivityScenario.launch(TrackActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        val item = SnapshotItemView(activity)
        val container = item.getChildAt(0) as SnapshotItemView.SnapshotItemContainerView
        val version = "1.0.0-${"1234567890".repeat(8)} (100) → 2.0.0-${"9876543210".repeat(8)} (200)"
        container.setVersionDisplay(version)
        container.setApiDisplay("API 24 → 37")
        val width = (320 * activity.resources.displayMetrics.density).toInt()
        item.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        item.layout(0, 0, item.measuredWidth, item.measuredHeight)
        val text = container.versionInfo
        assertTrue(text.lineCount > 1)
        assertEquals(version.length, text.layout.getLineEnd(text.lineCount - 1))
        for (line in 0 until text.lineCount) {
          assertEquals(0, text.layout.getEllipsisCount(line))
          assertTrue(text.layout.getLineMax(line) <= text.width)
        }
        assertTrue(text.height >= text.layout.height)
        assertTrue(container.apisInfo.top >= text.bottom)
      }
    }
  }

  @Test
  fun indicatorAlignsWithContentEndPadding() {
    ActivityScenario.launch(TrackActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        val item = SnapshotItemView(activity)
        val container = item.getChildAt(0) as SnapshotItemView.SnapshotItemContainerView
        container.stateIndicator.setSnapshotStateCounts(1, 0, 0, 0)
        item.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        item.layout(0, 0, item.measuredWidth, item.measuredHeight)
        val bitmap = Bitmap.createBitmap(item.width, item.height, Bitmap.Config.ARGB_8888)
        item.draw(Canvas(bitmap))
        val indicator = container.stateIndicator
        assertEquals(activity.resources.getDimensionPixelSize(R.dimen.main_list_horizontal_padding), container.width - indicator.right)
        assertEquals(activity.getColor(R.color.material_green_300), bitmap.getPixel(container.left + indicator.left + indicator.width / 2, container.top + indicator.top + indicator.height / 2))
        bitmap.recycle()
      }
    }
  }
}
