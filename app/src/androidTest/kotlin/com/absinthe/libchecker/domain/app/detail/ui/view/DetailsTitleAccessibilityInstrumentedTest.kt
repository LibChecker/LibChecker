package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Intent
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.app.detail.ui.adapter.node.AbiLabelNode
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import com.google.android.flexbox.FlexboxLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailsTitleAccessibilityInstrumentedTest {
  @Test
  fun badgesAreSeparateSpeakingNodesWithScreenBoundsInBothDirections() {
    withAttachedContainer { parent ->
      val context = ContextThemeWrapper(parent.context, R.style.AppTheme)
      val abis = listOf(
        Constants.ARMV8, Constants.ARMV7, Constants.ARMV5, Constants.X86_64,
        Constants.X86, Constants.MIPS64, Constants.MIPS, Constants.RISCV64,
        Constants.RISCV32, Constants.MULTI_ARCH
      )
      for (direction in listOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)) {
        for (tinted in listOf(false, true)) {
          var clicks = 0
          val title = DetailsTitleView(context).apply {
            layoutDirection = direction
            appNameView.text = "Example"
            packageNameView.text = "com.example"
            versionInfoView.text = "1.0"
            extraInfoView.text = "Size: 10 MB"
            setAbiLabels(
              abis.mapIndexed { index, abi ->
                AbiLabelNode(
                  abi = abi,
                  active = index % 2 == 0,
                  contentDescription = "ABI $abi",
                  is64Bit = index % 2 == 0,
                  action = if (abi == Constants.MULTI_ARCH) ({ clicks++ }) else null
                )
              },
              tintAbiLabels = tinted
            )
          }
          parent.addView(title)
          val badges = title.getChildAt(title.childCount - 1) as FlexboxLayout
          assertTrue(title.isAttachedToWindow)
          assertTrue(badges.getChildAt(0).isAttachedToWindow)
          // Exercise wrapping as well as the ordinary single-badge case below.
          val width = (240 * context.resources.displayMetrics.density).toInt()
          title.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), 0)
          title.layout(0, 0, title.measuredWidth, title.measuredHeight)
          assertTrue(badges.top >= title.extraInfoView.bottom)
          if (direction == View.LAYOUT_DIRECTION_RTL) {
            assertEquals(title.appNameView.right, badges.right)
          } else {
            assertEquals(title.appNameView.left, badges.left)
          }
          for (index in abis.indices) {
            val badge = badges.getChildAt(index)
            val node = AccessibilityNodeInfoCompat.wrap(badge.createAccessibilityNodeInfo())
            assertEquals("ABI ${abis[index]}", node.contentDescription.toString())
            // A description alone lets TalkBack absorb this into a focusable ancestor.
            assertTrue(node.isScreenReaderFocusable)
            assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_YES, badge.importantForAccessibility)
            assertEquals(index % 2 == 0, badge.alpha == 1f)
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            val location = IntArray(2)
            badge.getLocationOnScreen(location)
            assertEquals(Rect(location[0], location[1], location[0] + badge.width, location[1] + badge.height), bounds)
            assertEquals((42 * context.resources.displayMetrics.density + 0.5f).toInt(), bounds.width())
            assertEquals((28 * context.resources.displayMetrics.density + 0.5f).toInt(), bounds.height())
            val titleLocation = IntArray(2)
            title.getLocationOnScreen(titleLocation)
            bounds.offset(-titleLocation[0], -titleLocation[1])
            assertTrue(bounds.top >= title.extraInfoView.bottom)
            assertFalse(bounds.contains(title.iconView.left, title.iconView.top))
            assertEquals(abis[index] == Constants.MULTI_ARCH, node.isClickable)
          }
          badges.getChildAt(abis.lastIndex).performClick()
          assertEquals(1, clicks)
          for (visibility in listOf(View.INVISIBLE, View.GONE)) {
            val hiddenBadge = badges.getChildAt(0)
            hiddenBadge.visibility = visibility
            val accessibleChildren = ArrayList<View>()
            badges.addChildrenForAccessibility(accessibleChildren)
            assertFalse(accessibleChildren.contains(hiddenBadge))
            assertTrue(accessibleChildren.contains(badges.getChildAt(1)))
          }
          title.setAbiLabels(listOf(AbiLabelNode(Constants.ARMV8, true, "arm64-v8a", true)), tinted)
          assertEquals(1, badges.childCount)
          assertTrue(AccessibilityNodeInfoCompat.wrap(badges.getChildAt(0).createAccessibilityNodeInfo()).isScreenReaderFocusable)
          title.setAbiLabels(emptyList(), tinted)
          assertEquals(0, badges.childCount)
          parent.removeView(title)
        }
      }
    }
  }

  private fun withAttachedContainer(block: (FrameLayout) -> Unit) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val activity = instrumentation.startActivitySync(
      Intent(instrumentation.targetContext, TrackActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    try {
      lateinit var parent: FrameLayout
      instrumentation.runOnMainSync {
        parent = FrameLayout(activity)
        activity.addContentView(
          parent,
          ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
      }
      instrumentation.waitForIdleSync()
      instrumentation.runOnMainSync {
        // View.includeForAccessibility() excludes every child without window attachment.
        assertTrue(parent.isAttachedToWindow)
        assertTrue(parent.width > 0 && parent.height > 0)
        block(parent)
      }
    } finally {
      instrumentation.runOnMainSync { activity.finish() }
    }
  }
}
