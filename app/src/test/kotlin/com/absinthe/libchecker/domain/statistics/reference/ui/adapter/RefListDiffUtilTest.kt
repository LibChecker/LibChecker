package com.absinthe.libchecker.domain.statistics.reference.ui.adapter

import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefListDiffUtilTest {
  @Test
  fun keepsReferenceIdentityAndDetectsMembershipChanges() {
    val diff = RefListDiffUtil()
    val reference = LibReference("sample", null, linkedSetOf("one", "two"), NATIVE)
    val reordered = reference.copy(referredList = linkedSetOf("two", "one"))
    val replaced = reference.copy(referredList = setOf("one", "three"))
    val differentType = reference.copy(type = PACKAGE)

    assertTrue(diff.areItemsTheSame(reference, reordered))
    assertTrue(diff.areContentsTheSame(reference, reordered))
    assertTrue(diff.areItemsTheSame(reference, replaced))
    assertFalse(diff.areContentsTheSame(reference, replaced))
    assertFalse(diff.areItemsTheSame(reference, differentType))
    assertFalse(diff.areContentsTheSame(reference, differentType))
    assertFalse(diff.areItemsTheSame(reference, reference.copy(libName = "other")))
    assertFalse(diff.areContentsTheSame(reference, reference.copy(referredList = setOf("one"))))
  }
}
