package com.absinthe.libchecker.domain.app.detail.presentation

import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.domain.app.repository.AppDetailSettingsRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailFilterControllerTest {

  private val controller = DetailFilterController(FakeAppDetailSettingsRepository())

  @Test
  fun `component process map makes process action visible`() {
    val data = controller.buildProcessFilterData(
      type = ACTIVITY,
      componentProcessesMap = mapOf("com.example:worker" to 1),
      permissionItems = null,
      permissionNotGrantedLabel = "Not granted",
      permissionNotGrantedColor = 2
    )

    assertTrue(data.processToolIconVisible)
  }

  @Test
  fun `empty component process map keeps process action hidden`() {
    val data = controller.buildProcessFilterData(
      type = ACTIVITY,
      componentProcessesMap = emptyMap(),
      permissionItems = null,
      permissionNotGrantedLabel = "Not granted",
      permissionNotGrantedColor = 2
    )

    assertFalse(data.processToolIconVisible)
  }
}

private class FakeAppDetailSettingsRepository : AppDetailSettingsRepository {
  override val sortMode: Int = 0
  override val processMode: Boolean = false

  override fun setSortMode(sortMode: Int) = Unit

  override fun setProcessMode(enabled: Boolean) = Unit
}
