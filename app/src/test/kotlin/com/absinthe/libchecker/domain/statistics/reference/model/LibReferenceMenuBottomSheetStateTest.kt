package com.absinthe.libchecker.domain.statistics.reference.model

import com.absinthe.libchecker.constant.options.LibReferenceOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibReferenceMenuBottomSheetStateTest {

  @Test
  fun `builds ordered reference options and checked state`() {
    val state = buildLibReferenceMenuBottomSheetState(
      currentOptions = LibReferenceOptions.NATIVE_LIBS or LibReferenceOptions.PERMISSIONS
    )

    assertEquals(
      listOf(
        LibReferenceOptions.NATIVE_LIBS,
        LibReferenceOptions.SERVICES,
        LibReferenceOptions.ACTIVITIES,
        LibReferenceOptions.RECEIVERS,
        LibReferenceOptions.PROVIDERS,
        LibReferenceOptions.ACTION,
        LibReferenceOptions.PERMISSIONS,
        LibReferenceOptions.METADATA,
        LibReferenceOptions.PACKAGES,
        LibReferenceOptions.SHARED_UID,
        LibReferenceOptions.ONLY_NOT_MARKED
      ),
      state.options.map { it.option }
    )
    assertTrue(state.options.first().isChecked)
    assertTrue(state.options[6].isChecked)
    assertFalse(state.options.last().isChecked)
  }
}
