package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter

import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailRuleChipDisplayData
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotDetailInteractionPolicyTest {

  @Test
  fun removedRowsDoNotOpenDetail() {
    val row = buildRow(
      buildDisplayData(
        name = "com.example.RemovedService",
        itemType = SERVICE,
        diffType = REMOVED
      )
    )

    assertFalse(row.interactionPolicy(ownerPackageName = "com.example.app").opensDetail)
  }

  @Test
  fun dexRowsDoNotOpenReference() {
    val row = buildRow(
      buildDisplayData(
        name = "classes.dex",
        itemType = DEX
      )
    )

    assertFalse(row.interactionPolicy(ownerPackageName = "com.example.app").opensReference)
  }

  @Test
  fun ownerPackageComponentsDoNotOpenReference() {
    val row = buildRow(
      buildDisplayData(
        name = "com.example.app.SyncService",
        itemType = SERVICE
      )
    )

    assertFalse(row.interactionPolicy(ownerPackageName = "com.example.app").opensReference)
  }

  @Test
  fun externalComponentsOpenReferenceAndPreserveRuleChipMetadata() {
    val row = buildRow(
      buildDisplayData(
        name = "androidx.startup.InitializationProvider",
        itemType = SERVICE,
        ruleChip = SnapshotDetailRuleChipDisplayData(
          label = "Jetpack App Startup",
          iconRes = 20,
          regexName = "androidx.startup.*",
          isSimpleColorIcon = false,
          useColorfulIcon = false
        )
      )
    )

    val policy = row.interactionPolicy(ownerPackageName = "com.example.app")

    assertTrue(policy.opensReference)
    assertEquals("Jetpack App Startup", policy.referenceLabel)
    assertEquals("Jetpack App Startup", policy.ruleChipLabel)
    assertEquals("androidx.startup.*", policy.ruleChipRegexName)
  }

  private fun buildRow(displayData: SnapshotDetailItemDisplayData): SnapshotDetailRow.Item {
    return SnapshotDetailRow.Item(
      sectionType = displayData.item.itemType,
      displayData = displayData
    )
  }

  private fun buildDisplayData(
    name: String,
    itemType: Int,
    diffType: Int = ADDED,
    ruleChip: SnapshotDetailRuleChipDisplayData? = null
  ): SnapshotDetailItemDisplayData {
    return SnapshotDetailItemDisplayData(
      item = SnapshotDetailItem(
        name = name,
        title = name,
        extra = "",
        diffType = diffType,
        itemType = itemType
      ),
      title = name,
      extra = "",
      description = name,
      reportText = "item report\n",
      status = SnapshotDetailItemStatusDisplayData(
        iconRes = 10,
        colorRes = 11,
        labelRes = 12
      ),
      ruleChip = ruleChip
    )
  }
}
