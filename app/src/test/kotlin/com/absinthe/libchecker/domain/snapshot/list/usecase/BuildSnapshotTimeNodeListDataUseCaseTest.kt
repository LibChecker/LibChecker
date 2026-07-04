package com.absinthe.libchecker.domain.snapshot.list.usecase

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.lifecycle.LifecycleOwner
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.app.AppInstallSource
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.app.PackageChangeState
import com.absinthe.libchecker.domain.app.list.model.InstalledPackageState
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildSnapshotTimeNodeListDataUseCaseTest {

  @Test
  fun buildsTimeNodeDisplayTextAndDescription() = runBlocking {
    val useCase = BuildSnapshotTimeNodeListDataUseCase(
      getSnapshotPackageIconSources = GetSnapshotPackageIconSourcesUseCase(
        installedAppRepository = FakeInstalledAppRepository()
      ),
      formatTimestamp = { timestamp -> "formatted:$timestamp" }
    )

    val result = useCase(
      listOf(
        TimeStampItem(
          timestamp = 1234L,
          topApps = "[\"com.example.alpha\",\"com.example.beta\"]",
          systemProps = null
        )
      )
    )

    assertEquals(1, result.items.size)
    assertEquals(1234L, result.items.single().timestamp)
    assertEquals("formatted:1234", result.items.single().timestampText)
    assertEquals("formatted:1234", result.items.single().description)
    assertEquals(
      listOf("com.example.alpha", "com.example.beta"),
      result.items.single().topAppPackageNames
    )
    assertEquals(
      mapOf(
        "com.example.alpha" to SnapshotPackageIconSource.Fallback,
        "com.example.beta" to SnapshotPackageIconSource.Fallback
      ),
      result.packageIconSources
    )
  }
}

private class FakeInstalledAppRepository : InstalledAppRepository {

  override val packageChanges: SharedFlow<PackageChangeState> = MutableSharedFlow()

  override fun getApplicationList(forceUpdate: Boolean): List<PackageInfo> = emptyList()

  override fun getApplicationMap(forceUpdate: Boolean): Map<String, PackageInfo> = emptyMap()

  override fun getApplicationCount(forceUpdate: Boolean): Int = 0

  override fun getRandomApplicationInfo(forceUpdate: Boolean): ApplicationInfo? = null

  override fun getApexPackageNames(): Set<String> = emptySet()

  override fun startPackageChangeMonitoring(owner: LifecycleOwner) = Unit

  override fun stopPackageChangeMonitoring(owner: LifecycleOwner) = Unit

  override fun getPackageInfo(
    packageName: String,
    flags: Int,
    resolveFrozenArchiveInfo: Boolean
  ): PackageInfo? = null

  override fun isPackageInstalled(packageName: String): Boolean = false

  override fun isPackagePreinstalled(packageName: String): Boolean = false

  override fun getInstallSource(packageName: String): AppInstallSource? = null

  override fun getPermissions(packageName: String): List<String> = emptyList()

  override fun getPackageState(packageName: String): InstalledPackageState {
    return InstalledPackageState(packageInfo = null, isFrozen = false)
  }
}
