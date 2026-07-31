package com.absinthe.libchecker.domain.statistics.reference.model

import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.SHARED_UID
import com.absinthe.rulesbundle.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibReferenceItemDisplayTest {
  private val searchLabels = LibReferenceSearchLabels(
    notMarkedLabel = "Not marked",
    permissionFallbackLabel = "Permission",
    metadataLabel = "Metadata",
    packageLabel = "Package"
  )

  @Test
  fun buildsMarkedReferenceDisplay() {
    val rule = Rule(
      "libsample.so",
      NATIVE,
      "Sample SDK",
      42,
      null,
      null,
      false
    )

    val display = LibReferenceItemDisplay.create(
      reference = LibReference(
        libName = "libsample.so",
        rule = rule,
        referredList = setOf("one", "two"),
        type = NATIVE
      ),
      colorfulRuleIcon = false,
      notMarkedLabel = "Not marked",
      permissionFallbackLabel = "Permission",
      metadataLabel = "Metadata",
      countText = "2"
    )

    assertEquals("Sample SDK", display.label)
    assertFalse(display.italicLabel)
    assertEquals("libsample.so", display.libName)
    assertEquals("2", display.count)
    assertEquals(42, display.iconRes)
    assertEquals("Sample SDK", display.iconContentDescription)
    assertEquals(!rule.isSimpleColorIcon, display.desaturateIcon)
    assertTrue(display.canOpenDetail)
    assertEquals("Sample SDK, libsample.so, 2", display.contentDescription)
  }

  @Test
  fun buildsAndroidPermissionDisplayWithPermissionLabel() {
    val display = LibReferenceItemDisplay.create(
      reference = LibReference(
        libName = "android.permission.CAMERA",
        rule = null,
        referredList = setOf("one"),
        type = PERMISSION,
        resolvedLabel = "Camera"
      ),
      colorfulRuleIcon = true,
      notMarkedLabel = "Not marked",
      permissionFallbackLabel = "Permission",
      metadataLabel = "Metadata",
      countText = "1"
    )

    assertEquals("Camera", display.label)
    assertFalse(display.italicLabel)
    assertEquals(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android, display.iconRes)
    assertEquals("android.permission.CAMERA", display.iconContentDescription)
    assertFalse(display.desaturateIcon)
    assertFalse(display.canOpenDetail)
    assertEquals(
      "Camera, android.permission.CAMERA, 1",
      display.contentDescription
    )
  }

  @Test
  fun buildsUnknownPermissionDisplayWithPermissionFallbackLabel() {
    val display = LibReferenceItemDisplay.create(
      reference = LibReference(
        libName = "com.example.permission.UNKNOWN",
        rule = null,
        referredList = setOf("one"),
        type = PERMISSION
      ),
      colorfulRuleIcon = true,
      notMarkedLabel = "Not marked",
      permissionFallbackLabel = "Permission",
      metadataLabel = "Metadata",
      countText = "1"
    )

    assertEquals("Permission", display.label)
    assertFalse(display.italicLabel)
    assertEquals(
      "Permission, com.example.permission.UNKNOWN, 1",
      display.contentDescription
    )
  }

  @Test
  fun buildsPermissionDisplayWithFallbackWhenLabelMatchesPermissionName() {
    val permissionName = "android.permission.INTERNET"
    val display = LibReferenceItemDisplay.create(
      reference = LibReference(
        libName = permissionName,
        rule = null,
        referredList = setOf("one"),
        type = PERMISSION,
        resolvedLabel = permissionName
      ),
      colorfulRuleIcon = true,
      notMarkedLabel = "Not marked",
      permissionFallbackLabel = "Permission",
      metadataLabel = "Metadata",
      countText = "1"
    )

    assertEquals("Permission", display.label)
    assertFalse(display.italicLabel)
  }

  @Test
  fun buildsMetadataDisplayWithMetadataLabel() {
    val display = LibReferenceItemDisplay.create(
      reference = LibReference(
        libName = "com.example.feature",
        rule = null,
        referredList = setOf("one"),
        type = METADATA
      ),
      colorfulRuleIcon = true,
      notMarkedLabel = "Not marked",
      permissionFallbackLabel = "Permission",
      metadataLabel = "Metadata",
      countText = "1"
    )

    assertEquals("Metadata", display.label)
    assertFalse(display.italicLabel)
    assertEquals(R.drawable.ic_question, display.iconRes)
    assertEquals("Metadata, com.example.feature, 1", display.contentDescription)
  }

  @Test
  fun buildsPackageGroupDisplayWithWildcardSuffix() {
    val display = MultipleAppsIconItemDisplay.create(
      reference = LibReference(
        libName = "com.example",
        rule = null,
        referredList = setOf("one", "two"),
        type = PACKAGE
      ),
      notMarkedLabel = "Not marked",
      packageLabel = "Package",
      sharedUidLabel = "UID 1000"
    )

    assertEquals("Package", display.label)
    assertFalse(display.italicLabel)
    assertEquals("com.example.*", display.libName)
    assertEquals("2", display.count)
    assertTrue(display.iconPackages.isEmpty())
    assertEquals("Package, com.example.*, 2", display.contentDescription)
  }

  @Test
  fun buildsSharedUidDisplayWithUidValueInLabel() {
    val display = MultipleAppsIconItemDisplay.create(
      reference = LibReference(
        libName = "android.uid.system",
        rule = null,
        referredList = setOf("one", "two"),
        type = SHARED_UID
      ),
      notMarkedLabel = "Not marked",
      packageLabel = "Package",
      sharedUidLabel = "UID 1000"
    )

    assertEquals("UID 1000", display.label)
    assertFalse(display.italicLabel)
    assertEquals("android.uid.system", display.libName)
    assertEquals("2", display.count)
    assertEquals("UID 1000, android.uid.system, 2", display.contentDescription)
  }

  @Test
  fun searchesResolvedPermissionLabel() {
    val reference = LibReference(
      libName = "android.permission.CAMERA",
      rule = null,
      referredList = setOf("one"),
      type = PERMISSION,
      resolvedLabel = "Camera"
    )

    assertTrue(reference.matchesSearchQuery("camera", searchLabels))
  }

  @Test
  fun searchesVisibleCategoryLabels() {
    val metadata = LibReference(
      libName = "com.example.feature",
      rule = null,
      referredList = setOf("one"),
      type = METADATA
    )
    val packageGroup = LibReference(
      libName = "com.example",
      rule = null,
      referredList = setOf("one"),
      type = PACKAGE
    )
    val sharedUid = LibReference(
      libName = "android.uid.system",
      rule = null,
      referredList = setOf("one"),
      type = SHARED_UID
    )

    assertTrue(metadata.matchesSearchQuery("metadata", searchLabels))
    assertTrue(packageGroup.matchesSearchQuery("package", searchLabels))
    assertTrue(sharedUid.matchesSearchQuery("uid", searchLabels))
  }
}
