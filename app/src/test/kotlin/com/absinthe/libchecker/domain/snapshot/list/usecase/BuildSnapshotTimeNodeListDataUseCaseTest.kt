package com.absinthe.libchecker.domain.snapshot.list.usecase

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.lifecycle.LifecycleOwner
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.app.list.model.InstalledPackageState
import com.absinthe.libchecker.domain.app.model.AppInstallSource
import com.absinthe.libchecker.domain.app.model.PackageChangeState
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotRepresentativeApps
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildSnapshotTimeNodeListDataUseCaseTest {

  @Test
  fun ignoresUninstalledPackageRecordsWhenResolvingIcons() = runBlocking {
    val installedPackage = PackageInfo().apply { packageName = "com.example.installed" }
    val uninstalledPackage = PackageInfo().apply { packageName = "com.example.uninstalled" }
    val useCase = GetSnapshotPackageIconSourcesUseCase(
      installedAppRepository = FakeInstalledAppRepository(
        applications = mapOf(installedPackage.packageName to installedPackage),
        packageInfos = mapOf(
          installedPackage.packageName to installedPackage,
          uninstalledPackage.packageName to uninstalledPackage
        )
      )
    )

    val result = useCase(listOf(installedPackage.packageName, uninstalledPackage.packageName))

    assertEquals(SnapshotPackageIconSource.InstalledPackage(installedPackage), result[installedPackage.packageName])
    assertEquals(SnapshotPackageIconSource.Fallback, result[uninstalledPackage.packageName])
  }

  @Test
  fun buildsTimeNodeDisplayTextAndFiltersUnavailableIcons() = runBlocking {
    val installedPackage = PackageInfo().apply {
      packageName = "com.example.alpha"
    }
    val useCase = BuildSnapshotTimeNodeListDataUseCase(
      getSnapshotPackageIconSources = GetSnapshotPackageIconSourcesUseCase(
        installedAppRepository = FakeInstalledAppRepository(
          applications = mapOf(installedPackage.packageName to installedPackage)
        )
      ),
      getSnapshotCountsByTimestamp = { mapOf(1234L to 186) },
      formatTimestamp = { timestamp -> "formatted:$timestamp" }
    )

    val result = useCase(
      timeStamps = listOf(
        TimeStampItem(
          timestamp = 1234L,
          topApps = "[\"com.example.alpha\",\"com.example.beta\"]",
          systemProps = null
        )
      ),
      currentTimestamp = 1234L
    )

    assertEquals(1, result.items.size)
    assertEquals(1234L, result.items.single().timestamp)
    assertEquals("formatted:1234", result.items.single().timestampText)
    assertEquals("formatted:1234", result.items.single().description)
    assertEquals(186, result.items.single().appCount)
    assertEquals(true, result.items.single().isCurrent)
    assertEquals(
      listOf("com.example.alpha"),
      result.items.single().topAppPackageNames
    )
    assertEquals(
      mapOf(
        "com.example.alpha" to SnapshotPackageIconSource.InstalledPackage(installedPackage)
      ),
      result.packageIconSources
    )
  }

  @Test
  fun keepsOneOverflowCandidateForMoreIndicator() = runBlocking {
    val installedPackages = (1..7).associate { index ->
      val packageInfo = PackageInfo().apply {
        packageName = "com.example.$index"
      }
      packageInfo.packageName to packageInfo
    }
    val useCase = BuildSnapshotTimeNodeListDataUseCase(
      getSnapshotPackageIconSources = GetSnapshotPackageIconSourcesUseCase(
        installedAppRepository = FakeInstalledAppRepository(
          applications = installedPackages
        )
      )
    )

    val result = useCase(
      listOf(
        TimeStampItem(
          timestamp = 1234L,
          topApps = SnapshotRepresentativeApps.encode(installedPackages.keys.toList()),
          systemProps = null
        )
      )
    )

    assertEquals(7, result.items.single().topAppPackageNames.size)
    assertEquals(installedPackages.keys.take(7), result.items.single().topAppPackageNames)
  }
}

private class FakeInstalledAppRepository(
  private val applications: Map<String, PackageInfo> = emptyMap(),
  private val packageInfos: Map<String, PackageInfo> = emptyMap()
) : InstalledAppRepository {

  override val packageChanges: SharedFlow<PackageChangeState> = MutableSharedFlow()

  override fun getApplicationList(forceUpdate: Boolean): List<PackageInfo> = emptyList()

  override fun getApplicationMap(forceUpdate: Boolean): Map<String, PackageInfo> = applications

  override fun getApplicationCount(forceUpdate: Boolean): Int = 0

  override fun getRandomApplicationInfo(forceUpdate: Boolean): ApplicationInfo? = null

  override fun getApexPackageNames(): Set<String> = emptySet()

  override fun startPackageChangeMonitoring(owner: LifecycleOwner) = Unit

  override fun stopPackageChangeMonitoring(owner: LifecycleOwner) = Unit

  override fun getPackageInfo(
    packageName: String,
    flags: Int,
    resolveFrozenArchiveInfo: Boolean
  ): PackageInfo? = packageInfos[packageName]

  override fun isPackageInstalled(packageName: String): Boolean = false

  override fun isPackagePreinstalled(packageName: String): Boolean = false

  override fun getInstallSource(packageName: String): AppInstallSource? = null

  override fun getPermissions(packageName: String): List<String> = emptyList()

  override fun getPackageState(packageName: String): InstalledPackageState {
    return InstalledPackageState(packageInfo = null, isFrozen = false)
  }
}
