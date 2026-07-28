package com.absinthe.libchecker.utils

import android.content.pm.PackageManager
import com.absinthe.libchecker.utils.ShizukuManager.Availability
import org.junit.Assert.assertEquals
import org.junit.Test

class ShizukuManagerTest {

  @Test
  fun `active compatible binder reaches permission check without manager package lookup`() {
    val availability = ShizukuManager.getAvailability(
      pingBinder = { true },
      getVersion = { 13 },
      checkSelfPermission = { PackageManager.PERMISSION_DENIED }
    )

    assertEquals(Availability.PermissionDenied, availability)
  }

  @Test
  fun `inactive binder does not query version or permission`() {
    val availability = ShizukuManager.getAvailability(
      pingBinder = { false },
      getVersion = { error("Version must not be queried without a binder") },
      checkSelfPermission = { error("Permission must not be queried without a binder") }
    )

    assertEquals(Availability.NotRunning, availability)
  }
}
