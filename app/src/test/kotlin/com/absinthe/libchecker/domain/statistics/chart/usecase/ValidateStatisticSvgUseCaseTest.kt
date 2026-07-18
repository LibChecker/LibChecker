package com.absinthe.libchecker.domain.statistics.chart.usecase

import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateStatisticSvgUseCaseTest {

  private val validate = ValidateStatisticSvgUseCase()

  @Test
  fun `accepts a small path-only SVG`() {
    val errors = validate(
      """
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
        <path fill="#000000" d="M4 20V10h4v10H4Z"/>
      </svg>
      """.trimIndent().encodeToByteArray()
    )

    assertTrue(errors.toString(), errors.isEmpty())
  }

  @Test
  fun `rejects executable and externally referenced content`() {
    val errors = validate(
      """
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" onload="run()">
        <script>run()</script>
        <image href="https://example.com/icon.png"/>
      </svg>
      """.trimIndent().encodeToByteArray()
    )

    assertTrue(errors.any { "forbidden content" in it })
    assertTrue(errors.any { "event attribute" in it })
    assertTrue(errors.any { "unsupported element" in it })
  }

  @Test
  fun `rejects invalid viewBox and oversized document`() {
    val invalidViewBox = validate("<svg viewBox=\"0 0 0 24\"></svg>".encodeToByteArray())
    val oversized = validate(ByteArray(64 * 1024 + 1))

    assertTrue(invalidViewBox.any { "viewBox" in it })
    assertTrue(oversized.any { "exceeds" in it })
  }
}
