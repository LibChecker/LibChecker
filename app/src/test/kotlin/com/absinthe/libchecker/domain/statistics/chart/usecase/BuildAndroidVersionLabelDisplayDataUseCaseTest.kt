package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.domain.statistics.chart.model.AndroidVersionLabelDisplayData
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildAndroidVersionLabelDisplayDataUseCaseTest {

  private val useCase = BuildAndroidVersionLabelDisplayDataUseCase(
    sdkInt = 35,
    formatReleaseDate = { "2024-01" }
  )

  @Test
  fun `builds final icon text and description`() {
    val display = useCase(
      AndroidVersions.Node(
        version = 35,
        codeName = "Vanilla Ice Cream",
        versionName = "15",
        iconRes = 123,
        releaseDate = Date(0)
      )
    )

    assertEquals(
      AndroidVersionLabelDisplayData(
        iconRes = 123,
        text = "Vanilla Ice Cream, 15, 2024-01"
      ),
      display
    )
  }

  @Test
  fun `omits empty version name and supports missing node`() {
    val display = useCase(
      AndroidVersions.Node(
        version = 1,
        codeName = "Base",
        versionName = "",
        iconRes = null,
        releaseDate = Date(0)
      )
    )

    assertEquals("Base, 2024-01", display?.text)
    assertNull(useCase(null))
  }
}
