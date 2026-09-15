package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.domain.statistics.chart.model.AndroidVersionLabelDisplayData
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildAndroidVersionLabelDisplayDataUseCaseTest {

  @Test
  fun `builds label from the initialized Android version catalog`() {
    val node = AndroidVersions.versions.single { it.version == 35 }
    assertEquals("15", AndroidVersions.simpleVersions[35])
    assertEquals("Vanilla Ice Cream, 15, 2024-01", useCase(node)?.text)
  }

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

  @Test
  fun `formats release date using default formatter`() {
    val defaultUseCase = BuildAndroidVersionLabelDisplayDataUseCase(sdkInt = 35)
    val display = defaultUseCase(
      AndroidVersions.Node(
        version = 35,
        codeName = "Vanilla Ice Cream",
        versionName = "15",
        iconRes = 123,
        releaseDate = Date(1704067200000L) // 2024-01-01 UTC
      )
    )
    // Release date is formatted as yyyy-MM
    assertEquals(true, display?.text?.startsWith("Vanilla Ice Cream, 15, "))
  }
}
