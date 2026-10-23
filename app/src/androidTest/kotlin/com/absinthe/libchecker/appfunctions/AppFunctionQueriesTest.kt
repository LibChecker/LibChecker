package com.absinthe.libchecker.appfunctions

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CancellationSignal
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.internal.AppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.data.app.AndroidAppListItemFactory
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.app.detail.feature.GetAppDetailFeaturesUseCase
import com.absinthe.libchecker.domain.app.list.AppListItemFactory
import com.absinthe.libchecker.domain.app.model.VersionedFeature
import com.absinthe.libchecker.domain.app.repository.AppListRepository
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.rules.CloudRulesRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotItemFactory
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotDiffsUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotItemsUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotListsUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotWithInstalledAppsUseCase
import com.absinthe.libchecker.domain.snapshot.track.usecase.CompareTrackedSnapshotListsUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.ComputeLibReferenceUseCase
import java.lang.reflect.Proxy
import java.util.function.Consumer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 36)
class AppFunctionQueriesTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private lateinit var container: KoinApplication
  private lateinit var service: TestService
  private val constructedPackages = mutableListOf<String>()
  private var cachedFeatures = -1
  private var missingTimestamp: Long? = null
  private var disappearOnRead: Long? = null
  private var keepTimestamp = false
  private var cancelComparison = false

  private val packages = listOf(
    packageInfo("test.z", "Other"),
    packageInfo("test.a", "Target system", system = true),
    packageInfo("test.c", "Target C"),
    packageInfo("test.b", "Target B")
  )

  @Before
  fun setUp() {
    val installed = fake<InstalledAppRepository> { method, args ->
      when (method) {
        "getApplicationList", "getInstalledPackages" -> packages

        "getPackageInfo" -> {
          val flags = args[1] as Int
          context.packageManager.getPackageInfo(context.packageName, flags).apply {
            applicationInfo = ApplicationInfo(applicationInfo!!).apply {
              metaData = if (flags and PackageManager.GET_META_DATA != 0) {
                Bundle().apply {
                  putBoolean("xposedmodule", true)
                  putString("org.chromium.webapk.shell_apk.test", "test")
                  putString("com.android.stamp.type", "STAMP_TYPE_DISTRIBUTION_APK")
                  putString("com.android.stamp.source", "https://play.google.com/store")
                }
              } else {
                null
              }
            }
          }
        }

        "getInstallSource" -> null

        else -> error("Unexpected installed-app operation: $method")
      }
    }
    val appList = fake<AppListRepository> { method, args ->
      check(method == "updateFeatures")
      cachedFeatures = args[1] as Int
      Unit
    }
    val snapshots = fake<SnapshotRepository> { method, args ->
      when (method) {
        "getTimeStamp" -> (args[0] as Long).let {
          if (it == missingTimestamp && !keepTimestamp) null else TimeStampItem(it, null, null)
        }

        "getSnapshotCountsByTimestamp" -> listOf(1L, 2L).associateWith { if (it == missingTimestamp) 0 else 1 }

        "getSnapshots" -> {
          if (cancelComparison) throw CancellationException("test cancellation")
          val timestamp = args[0] as Long
          if (timestamp == disappearOnRead) missingTimestamp = timestamp
          if (timestamp == missingTimestamp) emptyList() else listOf(snapshot(timestamp))
        }

        "getTrackItems" -> emptyList<Any>()

        "deleteAllSnapshotDiffItems" -> Unit

        else -> error("Unexpected snapshot operation: $method")
      }
    }
    val compareItems = CompareSnapshotItemsUseCase()
    val compare = CompareSnapshotDiffsUseCase(
      snapshots,
      CompareTrackedSnapshotListsUseCase(snapshots, CompareSnapshotListsUseCase(compareItems)),
      CompareSnapshotWithInstalledAppsUseCase(
        context.packageManager,
        snapshots,
        installed,
        fake<SnapshotItemFactory> { _, _ -> error("Current comparison should stop at the missing baseline") },
        compareItems
      )
    )
    container = koinApplication {
      modules(
        module {
          single<InstalledAppRepository> { installed }
          single<SnapshotRepository> { snapshots }
          single { compare }
          single { ComputeLibReferenceUseCase(installed) }
          single { GetAppDetailFeaturesUseCase(appList, installed) }
          single<CloudRulesRepository> {
            fake { method, _ ->
              check(method == "getLocalVersion")
              48
            }
          }
          single<AppListItemFactory> {
            object : AppListItemFactory {
              override fun create(packageInfo: PackageInfo, delayInitFeatures: Boolean): LCItem {
                constructedPackages.add(packageInfo.packageName)
                if (packageInfo.packageName == context.packageName) {
                  return AndroidAppListItemFactory(context).create(packageInfo, delayInitFeatures)
                }
                return LCItem(
                  packageInfo.packageName, packageInfo.applicationInfo!!.nonLocalizedLabel.toString(),
                  "1", 1L, 0L, 0L,
                  packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                  0, 0, 36, 0
                )
              }
            }
          }
        }
      )
    }
    service = TestService(context, container.koin)
  }

  @After
  fun tearDown() {
    container.close()
  }

  @Test
  fun featureQueryKeepsMetadataDetectionsAndCachedBits() = runBlocking {
    cachedFeatures = Features.PWA or Features.XPOSED_MODULE or Features.PLAY_SIGNING
    val expectedBits = cachedFeatures
    val result = service.analyzeApp(context.packageName, category = "features")
    val names = result.evidence.map { it.name }.toSet()
    assertTrue(names.containsAll(setOf("pwa", "xposed", "play_signing")))
    assertEquals(expectedBits, cachedFeatures and expectedBits)
  }

  @Test
  fun featureQueryReturnsDetectedKotlinVersion() {
    val kotlin = VersionedFeature(Features.KOTLIN_USED, extras = mapOf("Kotlin" to "2.1.20", "Gradle" to "8.13"))
    assertEquals(EvidenceRecord("features", "kotlin", "2.1.20", ""), kotlin.toEvidence())
    assertEquals("1.8.0", VersionedFeature(Features.JETPACK_COMPOSE, version = "1.8.0").toEvidence().value)
    assertEquals("", VersionedFeature(Features.KOTLIN_USED).toEvidence().value)
  }

  @Test
  fun appQueryFiltersAndPagesBeforeConstructingRecords() = runBlocking {
    val page = service.findApps(query = "target", offset = 1, limit = 1)
    assertEquals(2, page.total)
    assertEquals(listOf("test.c"), page.apps.map { it.packageName })
    assertEquals(listOf("test.c"), constructedPackages)
    assertEquals(-1, page.nextOffset)
    constructedPackages.clear()
    assertTrue(service.findApps(query = "target", offset = 2, limit = 1).apps.isEmpty())
    assertTrue(constructedPackages.isEmpty())
  }

  @Test
  fun libraryQueryFiltersAndPagesBeforeConstructingRecords() = runBlocking {
    val page = service.findAppsUsingLibrary("android.permission.CAMERA", "permissions", offset = 1, limit = 1)
    assertEquals(2, page.total)
    assertEquals(listOf("test.c"), page.apps.map { it.packageName })
    assertEquals(listOf("test.c"), constructedPackages)
    assertEquals(-1, page.nextOffset)
  }

  @Test
  fun historicalComparisonRejectsBaselineDeletedAfterValidation() {
    disappearOnRead = 1L
    assertThrows(AppFunctionElementNotFoundException::class.java) {
      runBlocking { service.compareSnapshot(1L, 2L) }
    }
  }

  @Test
  fun historicalComparisonRejectsTargetDeletedAfterValidation() {
    disappearOnRead = 2L
    assertThrows(AppFunctionElementNotFoundException::class.java) {
      runBlocking { service.compareSnapshot(1L, 2L) }
    }
  }

  @Test
  fun currentComparisonRejectsBaselineDeletedAfterValidation() {
    disappearOnRead = 1L
    assertThrows(AppFunctionElementNotFoundException::class.java) {
      runBlocking { service.compareSnapshot(1L) }
    }
  }

  @Test
  fun currentComparisonRejectsRowsRemovedBeforeTimestampDeletion() {
    disappearOnRead = 1L
    keepTimestamp = true
    assertThrows(AppFunctionElementNotFoundException::class.java) {
      runBlocking { service.compareSnapshot(1L) }
    }
  }

  @Test
  fun unchangedSnapshotsReturnSuccessfulEmptyComparison() = runBlocking {
    val page = service.compareSnapshot(1L, 2L)
    assertTrue(page.changes.isEmpty())
    assertEquals(0, page.total)
    assertEquals(-1, page.nextOffset)
  }

  @Test
  fun comparisonCancellationIsNotReportedAsMissingSnapshot() {
    cancelComparison = true
    assertThrows(CancellationException::class.java) {
      runBlocking { service.compareSnapshot(1L, 2L) }
    }
  }

  private class TestService(context: Context, private val koin: Koin) :
    BaseAppFunctionService(),
    KoinComponent {
    init {
      attachBaseContext(context)
    }

    override fun getKoin(): Koin = koin

    override fun resolveInventory(): AppFunctionInventory = error("Test calls entry points directly")

    override fun onExecuteFunction(
      request: ExecuteAppFunctionRequest,
      metadata: AppFunctionMetadata,
      cancellationSignal: CancellationSignal,
      callback: Consumer<ExecuteAppFunctionResponse>
    ) = error("Test calls entry points directly")
  }

  private fun packageInfo(name: String, label: String, system: Boolean = false) = PackageInfo().apply {
    packageName = name
    applicationInfo = ApplicationInfo().apply {
      packageName = name
      nonLocalizedLabel = label
      flags = if (system) ApplicationInfo.FLAG_SYSTEM else 0
    }
    requestedPermissions = if (label.startsWith("Target")) arrayOf("android.permission.CAMERA") else emptyArray()
  }

  private fun snapshot(timestamp: Long) = SnapshotItem(
    id = null, packageName = "test.app", timeStamp = timestamp, label = "Test", versionName = "1",
    versionCode = 1, isArchived = false, installedTime = 1, lastUpdatedTime = 1, isSystem = false,
    abi = 0, targetApi = 36, nativeLibs = "[]", services = "[]", activities = "[]", receivers = "[]",
    providers = "[]", permissions = "[]", metadata = "[]", packageSize = 1, compileSdk = 36, minSdk = 24
  )

  private inline fun <reified T> fake(crossinline invoke: (String, Array<out Any?>) -> Any?): T {
    return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
      invoke(method.name, args.orEmpty())
    } as T
  }
}
