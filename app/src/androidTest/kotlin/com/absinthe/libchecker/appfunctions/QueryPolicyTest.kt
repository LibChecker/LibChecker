package com.absinthe.libchecker.appfunctions

import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.database.entity.Features
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QueryPolicyTest {
  @Test
  fun rejectsInvalidOrUnboundedRequests() {
    assertThrows(AppFunctionInvalidArgumentException::class.java) { validatePage(-1, 20) }
    assertThrows(AppFunctionInvalidArgumentException::class.java) { validatePage(0, 0) }
    assertThrows(AppFunctionInvalidArgumentException::class.java) { validatePage(0, 101) }
    assertThrows(AppFunctionInvalidArgumentException::class.java) { validateQuery(" ", required = true) }
    assertThrows(AppFunctionInvalidArgumentException::class.java) { validateQuery("x".repeat(201)) }
    assertThrows(AppFunctionInvalidArgumentException::class.java) { featureFlag("safe") }
    assertThrows(AppFunctionInvalidArgumentException::class.java) { referenceOption("private_data") }
  }

  @Test
  fun pagingHandlesExactBoundaryAndOverflow() {
    assertEquals(20, nextOffset(0, 20, 21))
    assertEquals(-1, nextOffset(0, 20, 20))
    assertEquals(-1, nextOffset(100, 20, 2))
    assertEquals(-1, nextOffset(Int.MAX_VALUE, 100, Int.MAX_VALUE))
    assertEquals(-1, nextOffset(0, 20, 0))
  }

  @Test
  fun featureNamesKeepDetectionSemantics() {
    assertNull(featureFlag(""))
    assertEquals(Features.COMPOSE_MULTIPLATFORM_RESOURCES, featureFlag("compose_resources"))
    assertEquals(Features.JETPACK_COMPOSE, featureFlag("compose"))
    assertEquals(LibReferenceOptions.NATIVE_LIBS, referenceOption("native"))
    assertEquals(LibReferenceOptions.PERMISSIONS, referenceOption("permissions"))
  }
}
