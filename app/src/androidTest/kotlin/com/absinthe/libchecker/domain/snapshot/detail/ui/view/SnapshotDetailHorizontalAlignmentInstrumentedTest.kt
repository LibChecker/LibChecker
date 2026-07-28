package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailItemViewRenderState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotDetailHorizontalAlignmentInstrumentedTest {

  @Test
  fun alignsExpansionArrowCenterWithStatusIndicatorCenter() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()

    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val titleView = SnapshotDetailTitleView(context)
      val widthSpec = View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY)
      val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

      titleView.measure(widthSpec, heightSpec)
      titleView.layout(0, 0, titleView.measuredWidth, titleView.measuredHeight)

      val titleText = titleView.getChildAt(TITLE_TEXT_CHILD_INDEX)
      val expansionArrow = titleView.getChildAt(EXPANSION_ARROW_CHILD_INDEX)
      assertTrue(expansionArrow.left < titleText.left)

      listOf(
        R.drawable.ic_add to R.string.snapshot_indicator_added,
        R.drawable.ic_changed to R.string.snapshot_indicator_changed,
        R.drawable.ic_move to R.string.snapshot_indicator_moved,
        R.drawable.ic_remove to R.string.snapshot_indicator_removed
      ).forEach { (iconRes, statusLabelRes) ->
        val itemView = SnapshotDetailItemView(context)
        itemView.render(
          SnapshotDetailItemViewRenderState(
            title = "Item",
            extra = "",
            iconRes = iconRes,
            statusColorRes = R.color.snapshot_status_added,
            statusLabelRes = statusLabelRes,
            contentDescription = "Snapshot item",
            ruleChip = null
          )
        )
        itemView.measure(widthSpec, heightSpec)
        itemView.layout(0, 0, itemView.measuredWidth, itemView.measuredHeight)

        val statusIndicator = itemView.getChildAt(STATUS_INDICATOR_CHILD_INDEX)
        assertEquals(
          expansionArrow.x + expansionArrow.width / 2f,
          statusIndicator.x + statusIndicator.width / 2f,
          0f
        )
      }
    }
  }

  private companion object {
    const val TITLE_TEXT_CHILD_INDEX = 0
    const val EXPANSION_ARROW_CHILD_INDEX = 2
    const val STATUS_INDICATOR_CHILD_INDEX = 1
  }
}
