package com.absinthe.libchecker.domain.statistics.reference.usecase

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.lifecycle.LifecycleOwner
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.domain.app.list.model.InstalledPackageState
import com.absinthe.libchecker.domain.app.model.AppInstallSource
import com.absinthe.libchecker.domain.app.model.PackageChangeState
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComputeLibReferenceUseCaseTest {

  @Test
  fun eachNarrowBatchIsConsumedBeforeLoadingTheNextType() = runBlocking {
    val target = target("app.one")
    val service = target("app.one").apply {
      services = arrayOf(ServiceInfo().apply { name = "sdk.Service" })
    }
    val activity = target("app.one").apply {
      activities = arrayOf(ActivityInfo().apply { name = "sdk.Activity" })
    }
    val flagsSeen = mutableListOf<Int>()
    val progress = mutableListOf<Int>()
    val flags = listOf(
      PackageManager.GET_SERVICES,
      PackageManager.GET_ACTIVITIES,
      PackageManager.GET_RECEIVERS,
      PackageManager.GET_PROVIDERS,
      PackageManager.GET_PERMISSIONS,
      PackageManager.GET_META_DATA
    )
    val repository = FakeInstalledAppRepository(listOf(target), loadBatch = { flag ->
      assertEquals(flags[flagsSeen.size], flag)
      if (flagsSeen.isNotEmpty()) {
        assertTrue(progress.last() > 0)
        // If service consumption is deferred until after the activity query,
        // the expected service reference disappears.
        service.services = null
      }
      flagsSeen += flag
      when (flag) {
        PackageManager.GET_SERVICES -> listOf(service)
        PackageManager.GET_ACTIVITIES -> listOf(activity)
        else -> listOf(target)
      }
    })
    val index = ComputeLibReferenceUseCase(repository).buildIndex(
      config(ALL_BATCH_OPTIONS),
      progress::add
    )!!

    assertEquals(flags, flagsSeen)
    assertEquals(
      mapOf("sdk.Service" to SERVICE, "sdk.Activity" to ACTIVITY),
      index.snapshotReferences().associate { it.name to it.type }
    )
    assertEquals(listOf(0, 17, 34, 50, 67, 84, 100), progress)
    assertTrue(index.packageInfoByName["app.one"] === target)
  }

  @Test
  fun multipleTypesMatchSingleTypeResultsWithFilteringAndDeduplication() = runBlocking {
    val targets = listOf(
      target("app.one"),
      target("app.two"),
      target("system.app", true),
      PackageInfo().apply { packageName = "missing.application" }
    )
    val repository = FakeInstalledAppRepository(targets, loadBatch = { flag ->
      targets.map { base ->
        target(base.packageName).apply {
          if (flag == PackageManager.GET_SERVICES) {
            services = arrayOf("sdk.Service", "sdk.Service", "${base.packageName}.Own").map {
              ServiceInfo().apply { name = it }
            }.toTypedArray()
          } else {
            activities = arrayOf(ActivityInfo().apply { name = "sdk.Activity" })
          }
        }
      }
    })
    val useCase = ComputeLibReferenceUseCase(repository)
    for (showSystem in listOf(false, true)) {
      suspend fun entries(options: Int) = useCase.buildIndex(config(options, showSystem)) {}!!
        .snapshotReferences().associate { it.name to (it.type to it.packageNames) }
      val expected = entries(LibReferenceOptions.SERVICES) + entries(LibReferenceOptions.ACTIVITIES)
      assertEquals(expected, entries(LibReferenceOptions.SERVICES or LibReferenceOptions.ACTIVITIES))
      val packages = setOf("app.one", "app.two") + if (showSystem) setOf("system.app") else emptySet()
      assertEquals(mapOf("sdk.Service" to (SERVICE to packages)), entries(LibReferenceOptions.SERVICES))
    }
  }

  @Test
  fun missingBatchPackagesUseNarrowFrozenArchiveFallbackAndSkipMissingPackages() = runBlocking {
    val calls = mutableListOf<Pair<String, Int>>()
    val repository = FakeInstalledAppRepository(
      listOf(target("frozen.app"), target("removed.app")),
      fallback = { name, flags, resolveArchive ->
        assertTrue(resolveArchive)
        calls += name to flags
        if (name == "removed.app") {
          null
        } else {
          target(name).apply {
            services = arrayOf(ServiceInfo().apply { this.name = "sdk.Service" })
            activities = arrayOf(ActivityInfo().apply { this.name = "sdk.Activity" })
          }
        }
      }
    )
    val index = ComputeLibReferenceUseCase(repository).buildIndex(
      config(LibReferenceOptions.SERVICES or LibReferenceOptions.ACTIVITIES)
    ) {}!!
    assertEquals(
      listOf(
        "frozen.app" to PackageManager.GET_SERVICES,
        "removed.app" to PackageManager.GET_SERVICES,
        "frozen.app" to PackageManager.GET_ACTIVITIES,
        "removed.app" to PackageManager.GET_ACTIVITIES
      ),
      calls
    )
    assertEquals(setOf("sdk.Service", "sdk.Activity"), index.snapshotReferences().map { it.name }.toSet())
    assertTrue(index.snapshotReferences().all { it.packageNames == setOf("frozen.app") })
  }

  @Test
  fun cancellationBetweenTypesDoesNotLoadAnotherBatch() = runBlocking {
    val flags = mutableListOf<Int>()
    val repository = FakeInstalledAppRepository(listOf(target("app.one")), loadBatch = {
      flags += it
      emptyList()
    })
    var returned = false
    val job = launch {
      val scanJob = coroutineContext[Job]!!
      ComputeLibReferenceUseCase(repository).buildIndex(
        config(LibReferenceOptions.SERVICES or LibReferenceOptions.ACTIVITIES)
      ) { if (it == 50) scanJob.cancel() }
      returned = true
    }
    job.join()
    assertTrue(job.isCancelled)
    assertFalse(returned)
    assertEquals(listOf(PackageManager.GET_SERVICES), flags)
  }

  @Test
  fun cancellationDuringBatchLoadStopsBeforeFallbackOrProgress() = runBlocking {
    val flags = mutableListOf<Int>()
    val progress = mutableListOf<Int>()
    val job = launch {
      val scanJob = coroutineContext[Job]!!
      val repository = FakeInstalledAppRepository(listOf(target("app.one")), loadBatch = {
        flags += it
        scanJob.cancel()
        emptyList()
      }, fallback = { _, _, _ -> error("Cancelled scan must not resolve fallback") })
      ComputeLibReferenceUseCase(repository).buildIndex(config(ALL_BATCH_OPTIONS), progress::add)
    }
    job.join()
    assertTrue(job.isCancelled)
    assertEquals(listOf(PackageManager.GET_SERVICES), flags)
    assertEquals(listOf(0), progress)
  }

  @Test
  fun permissionMatchingPreservesThresholdCountsAndDescendingOrder() = runBlocking {
    val targets = listOf(target("app.one"), target("app.two"))
    val repository = FakeInstalledAppRepository(targets, loadBatch = {
      targets.mapIndexed { i, base ->
        target(base.packageName).apply {
          requestedPermissions = if (i == 0) arrayOf("common", "common", "rare", "") else arrayOf("common")
        }
      }
    })
    val useCase = ComputeLibReferenceUseCase(repository)
    val index = useCase.buildIndex(config(LibReferenceOptions.PERMISSIONS)) {}!!
    val result = useCase.matchRules(index, ComputeLibReferenceUseCase.MatchConfig(1, true)) {}!!
    assertEquals(listOf("common", "rare"), result.map { it.libName })
    assertEquals(listOf(2, 1), result.map { it.referredList.size })
    assertTrue(result.all { it.type == PERMISSION })
    assertEquals(
      listOf("common"),
      useCase.matchRules(index, ComputeLibReferenceUseCase.MatchConfig(2, false)) {}!!
        .map { it.libName }
    )
    index.clear()
    assertTrue(index.snapshotReferences().isEmpty())
  }

  private fun target(name: String, system: Boolean = false) = PackageInfo().apply {
    packageName = name
    applicationInfo = ApplicationInfo().apply { flags = if (system) ApplicationInfo.FLAG_SYSTEM else 0 }
  }

  private fun config(options: Int, showSystem: Boolean = true) = ComputeLibReferenceUseCase.ReferenceConfig(showSystem, options)

  private companion object {
    const val ALL_BATCH_OPTIONS = LibReferenceOptions.SERVICES or LibReferenceOptions.ACTIVITIES or
      LibReferenceOptions.RECEIVERS or LibReferenceOptions.PROVIDERS or
      LibReferenceOptions.PERMISSIONS or LibReferenceOptions.METADATA
  }
}

private class FakeInstalledAppRepository(
  val targets: List<PackageInfo>,
  val loadBatch: (Int) -> List<PackageInfo> = { emptyList() },
  val fallback: (String, Int, Boolean) -> PackageInfo? = { _, _, _ -> null }
) : InstalledAppRepository {
  override val packageChanges: SharedFlow<PackageChangeState> = MutableSharedFlow()

  override fun getApplicationList(forceUpdate: Boolean): List<PackageInfo> {
    return targets
  }

  override fun getInstalledPackages(flags: Int): List<PackageInfo> = loadBatch(flags)

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
  ): PackageInfo? = fallback(packageName, flags, resolveFrozenArchiveInfo)

  override fun isPackageInstalled(packageName: String): Boolean = false

  override fun isPackagePreinstalled(packageName: String): Boolean = false

  override fun getInstallSource(packageName: String): AppInstallSource? = null

  override fun getPermissions(packageName: String): List<String> = emptyList()

  override fun getPackageState(packageName: String): InstalledPackageState {
    return InstalledPackageState(packageInfo = null, isFrozen = false)
  }
}
