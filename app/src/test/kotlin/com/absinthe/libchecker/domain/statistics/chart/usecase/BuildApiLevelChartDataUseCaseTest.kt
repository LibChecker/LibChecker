package com.absinthe.libchecker.domain.statistics.chart.usecase

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.lifecycle.LifecycleOwner
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.list.model.InstalledPackageState
import com.absinthe.libchecker.domain.app.model.AppInstallSource
import com.absinthe.libchecker.domain.app.model.PackageChangeState
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildApiLevelChartDataUseCaseTest {

  @Test
  fun `uses cached applicationMap before falling back to getPackageInfo`() = runBlocking {
    var getPackageInfoCalls = 0
    val cachedInfo = PackageInfo().apply {
      packageName = "app.cached"
      applicationInfo = ApplicationInfo().apply {
        targetSdkVersion = 35
        minSdkVersion = 26
      }
    }
    val fallbackInfo = PackageInfo().apply {
      packageName = "app.fallback"
      applicationInfo = ApplicationInfo().apply {
        targetSdkVersion = 34
        minSdkVersion = 24
      }
    }
    val repository = object : InstalledAppRepository {
      override val packageChanges: SharedFlow<PackageChangeState> = MutableSharedFlow()
      override fun getApplicationList(forceUpdate: Boolean): List<PackageInfo> = listOf(cachedInfo)
      override fun getInstalledPackages(flags: Int): List<PackageInfo> = emptyList()
      override fun getApplicationMap(forceUpdate: Boolean): Map<String, PackageInfo> = mapOf(cachedInfo.packageName to cachedInfo)
      override fun getApplicationCount(forceUpdate: Boolean): Int = 1
      override fun getRandomApplicationInfo(forceUpdate: Boolean): ApplicationInfo? = null
      override fun getApexPackageNames(): Set<String> = emptySet()
      override fun startPackageChangeMonitoring(owner: LifecycleOwner) = Unit
      override fun stopPackageChangeMonitoring(owner: LifecycleOwner) = Unit
      override fun getPackageInfo(
        packageName: String,
        flags: Int,
        resolveFrozenArchiveInfo: Boolean
      ): PackageInfo? {
        getPackageInfoCalls++
        return if (packageName == fallbackInfo.packageName) fallbackInfo else null
      }
      override fun isPackageInstalled(packageName: String): Boolean = true
      override fun isPackagePreinstalled(packageName: String): Boolean = false
      override fun getInstallSource(packageName: String): AppInstallSource? = null
      override fun getPermissions(packageName: String): List<String> = emptyList()
      override fun getPackageState(packageName: String): InstalledPackageState = InstalledPackageState(packageInfo = null, isFrozen = false)
    }

    val items = listOf(
      LCItem("app.cached", "Cached", "1.0", 1L, 0L, 0L, false, 0, 0, 35, 0),
      LCItem("app.fallback", "Fallback", "1.0", 1L, 0L, 0L, false, 0, 0, 34, 0)
    )
    val result = BuildApiLevelChartDataUseCase(repository)(
      BuildApiLevelChartDataUseCase.Request(
        items = items,
        kind = BuildApiLevelChartDataUseCase.Kind.TargetSdk,
        showSystemApps = true
      )
    )

    assertEquals(1, getPackageInfoCalls)
    assertEquals(listOf("app.fallback"), result[34]?.map { it.packageName })
    assertEquals(listOf("app.cached"), result[35]?.map { it.packageName })
  }
}
