package com.absinthe.libchecker.data.statistics

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticArtifactQuery
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class PackageEvidenceInstrumentedTest {
  @Test
  fun batchesPermissionLookupsAndResolvesOnlyThePrimaryIcon() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val installed = GlobalContext.get().get<InstalledAppRepository>()
    val info = checkNotNull(installed.getPackageInfo(context.packageName))
    var lookups = 0
    val counted = object : InstalledAppRepository by installed {
      override fun getPackageInfo(packageName: String, flags: Int, resolveFrozenArchiveInfo: Boolean): PackageInfo? {
        assertEquals(PackageManager.GET_PERMISSIONS, flags)
        assertEquals(false, resolveFrozenArchiveInfo)
        lookups++
        return checkNotNull(installed.getPackageInfo(packageName, flags, resolveFrozenArchiveInfo)).apply {
          requestedPermissions = arrayOf("example.permission.SPLIT", "example.permission.IMPLICIT")
          requestedPermissionsFlags = intArrayOf(0, PackageInfo.REQUESTED_PERMISSION_IMPLICIT)
        }
      }
    }
    val repository = AndroidStatisticEvidenceRepository(context.packageManager, counted)
    val explicit = StatisticArtifactQuery.RequestedPermissions(listOf("example.permission.SPLIT"))
    val missing = StatisticArtifactQuery.RequestedPermissions(listOf("example.permission.IMPLICIT"))
    val adaptive = StatisticArtifactQuery.ApplicationIconTraits(listOf("adaptive"))
    val queries = setOf(explicit, missing, adaptive)
    assertEquals(mapOf(explicit to true, missing to false, adaptive to true), repository.matchesAll(info.packageName, queries))
    assertEquals(1, lookups)

    val noIcon = PackageInfo().apply {
      packageName = info.packageName
      applicationInfo = ApplicationInfo(checkNotNull(info.applicationInfo)).apply { icon = 0 }
    }
    assertEquals(mapOf(adaptive to false), repository.matchesAll(noIcon, setOf(adaptive)))
    assertEquals(1, lookups)
  }
}
