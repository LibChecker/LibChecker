package com.absinthe.libchecker.domain.snapshot.album.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumItemDisplayDataTest {

  @Test
  fun displayDataCarriesRenderableAlbumItemState() {
    val displayData = AlbumItemDisplayData(
      iconRes = 1,
      iconBackgroundColorRes = 2,
      title = "Comparison",
      subtitle = "Compare with two snapshots",
      contentDescription = "Comparison, Compare with two snapshots",
      action = AlbumItemAction.Comparison
    )

    assertEquals(1, displayData.iconRes)
    assertEquals(2, displayData.iconBackgroundColorRes)
    assertEquals("Comparison", displayData.title)
    assertEquals("Compare with two snapshots", displayData.subtitle)
    assertEquals("Comparison, Compare with two snapshots", displayData.contentDescription)
    assertEquals(AlbumItemAction.Comparison, displayData.action)
  }

  @Test
  fun buildsAlbumItemDescriptionFromVisibleText() {
    assertEquals(
      "Comparison, Compare with two snapshots",
      buildAlbumItemDescription(
        title = "Comparison",
        subtitle = "Compare with two snapshots"
      )
    )
  }

  @Test
  fun skipsBlankAlbumItemDescriptionParts() {
    assertEquals(
      "Comparison",
      buildAlbumItemDescription(
        title = "Comparison",
        subtitle = " "
      )
    )
  }
}
