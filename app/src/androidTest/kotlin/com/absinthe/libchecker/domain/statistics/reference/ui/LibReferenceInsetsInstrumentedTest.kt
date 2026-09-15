package com.absinthe.libchecker.domain.statistics.reference.ui

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibReferenceInsetsInstrumentedTest {
  @Test
  fun visibleRootRetainsHorizontalSystemBarInsetsAfterContainerReplacement() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val intent = Intent(context, LibReferenceActivity::class.java)
      .putExtra(EXTRA_REF_NAME, "libc++_shared.so")
      .putExtra(EXTRA_REF_LIST, arrayOf("com.tencent.mm"))
    ActivityScenario.launch<LibReferenceActivity>(intent).use { scenario ->
      scenario.onActivity { activity ->
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        val visibleRoot = content.getChildAt(0)
        for (insets in listOf(Insets.of(72, 0, 0, 0), Insets.of(0, 0, 72, 0))) {
          ViewCompat.dispatchApplyWindowInsets(
            visibleRoot,
            WindowInsetsCompat.Builder()
              .setInsets(WindowInsetsCompat.Type.systemBars(), insets)
              .build()
          )
          assertEquals(insets.left, visibleRoot.paddingLeft)
          assertEquals(insets.right, visibleRoot.paddingRight)
        }
        assertEquals(View.VISIBLE, visibleRoot.visibility)
      }
    }
  }
}
