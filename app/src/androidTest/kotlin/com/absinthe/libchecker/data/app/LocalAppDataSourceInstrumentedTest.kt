package com.absinthe.libchecker.data.app

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.app.repository.PackageListLoadException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class LocalAppDataSourceInstrumentedTest {
  @Test
  fun failedRefreshKeepsLastCompleteCacheAndCanRetry() {
    val first = PackageInfo().apply {
      packageName = "first"
      applicationInfo = ApplicationInfo().apply { sourceDir = "/first.apk" }
    }
    var scan: () -> List<PackageInfo> = { listOf(first) }
    val source = LocalAppDataSource { scan() }
    val cached = source.getApplicationList(false)
    val failure = SecurityException("Package manager unavailable")
    scan = { throw failure }

    assertSame(
      failure,
      assertThrows(PackageListLoadException::class.java) {
        source.getApplicationList(true)
      }.cause
    )
    assertEquals(cached, source.getApplicationList(false))
    assertEquals(mapOf("first" to first), source.getApplicationMap(false))

    scan = { emptyList() }
    assertEquals(emptyList<PackageInfo>(), source.getApplicationList(true))
    assertEquals(0, source.getApplicationCount(false))
  }
}
