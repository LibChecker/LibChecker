package com.absinthe.libchecker.domain.statistics.reference.presentation

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.lifecycle.LifecycleOwner
import com.absinthe.libchecker.domain.app.list.model.InstalledPackageState
import com.absinthe.libchecker.domain.app.model.AppInstallSource
import com.absinthe.libchecker.domain.app.model.PackageChangeState
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceLoadingState
import com.absinthe.libchecker.domain.statistics.reference.repository.LibReferenceSettingsRepository
import com.absinthe.libchecker.domain.statistics.reference.repository.PermissionLabelResolver
import com.absinthe.libchecker.domain.statistics.reference.usecase.ComputeLibReferenceUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.GetLibReferenceConfigUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.GetLibReferenceIconPackagesUseCase
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibReferenceComputationControllerTest {

  @Test
  fun completedReferenceResultIsReplayedToLateCollector() = runBlocking {
    val repository = FakeInstalledAppRepository()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val controller = LibReferenceComputationController(
      scope = scope,
      computeLibReferenceUseCase = ComputeLibReferenceUseCase(repository),
      getLibReferenceIconPackagesUseCase = GetLibReferenceIconPackagesUseCase(repository),
      getLibReferenceConfigUseCase = GetLibReferenceConfigUseCase(FakeLibReferenceSettingsRepository()),
      permissionLabelResolver = PermissionLabelResolver { null },
      updateLoadingState = {}
    )

    controller.compute()
    withTimeout(TEST_TIMEOUT_MILLIS) {
      while (controller.savedRefList == null) {
        yield()
      }
    }

    val replayedResult = withTimeout(TEST_TIMEOUT_MILLIS) {
      controller.libReference.filterNotNull().first()
    }

    assertEquals(emptyList<Any>(), replayedResult)
    scope.cancel()
  }

  @Test
  fun replacementWaitsForCancelledPreparationAndOnlyPublishesLatestResult() = runBlocking {
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)
    val secondEntered = CountDownLatch(1)
    val states = Collections.synchronizedList(mutableListOf<LibReferenceLoadingState>())
    var calls = 0
    val repository = FakeInstalledAppRepository {
      calls++
      if (calls == 1) {
        entered.countDown()
        check(release.await(5, TimeUnit.SECONDS))
      } else {
        secondEntered.countDown()
      }
    }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val controller = LibReferenceComputationController(
      scope,
      ComputeLibReferenceUseCase(repository),
      GetLibReferenceIconPackagesUseCase(repository),
      GetLibReferenceConfigUseCase(FakeLibReferenceSettingsRepository()),
      PermissionLabelResolver { null },
      states::add
    )
    try {
      controller.compute()
      assertTrue(entered.await(5, TimeUnit.SECONDS))
      assertEquals(listOf(LibReferenceLoadingState.Preparing), states.toList())
      controller.compute()
      assertFalse(secondEntered.await(100, TimeUnit.MILLISECONDS))
      assertEquals(1, calls)
      assertEquals(null, controller.libReference.value)
      release.countDown()
      withTimeout(TEST_TIMEOUT_MILLIS) {
        controller.libReference.filterNotNull().first()
      }
      assertEquals(2, calls)
      assertEquals(
        listOf(
          LibReferenceLoadingState.Preparing,
          LibReferenceLoadingState.Preparing,
          LibReferenceLoadingState.Matching(),
          LibReferenceLoadingState.Organizing()
        ),
        states.toList()
      )
    } finally {
      release.countDown()
      scope.cancel()
    }
  }

  @Test
  fun refreshDoesNotExposeCachedResultDuringNewScan() = runBlocking {
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)
    var calls = 0
    val repository = FakeInstalledAppRepository {
      if (++calls == 2) {
        entered.countDown()
        check(release.await(5, TimeUnit.SECONDS))
      }
    }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val controller = LibReferenceComputationController(
      scope,
      ComputeLibReferenceUseCase(repository),
      GetLibReferenceIconPackagesUseCase(repository),
      GetLibReferenceConfigUseCase(FakeLibReferenceSettingsRepository()),
      PermissionLabelResolver { null },
      updateLoadingState = {}
    )
    try {
      controller.compute()
      withTimeout(TEST_TIMEOUT_MILLIS) { controller.libReference.filterNotNull().first() }
      controller.compute()
      assertTrue(entered.await(5, TimeUnit.SECONDS))
      controller.refresh().join()
      assertEquals(null, controller.libReference.value)
      release.countDown()
      val result = withTimeout(TEST_TIMEOUT_MILLIS) { controller.libReference.filterNotNull().first() }
      assertEquals(emptyList<Any>(), result)
    } finally {
      release.countDown()
      scope.cancel()
    }
  }

  private companion object {
    const val TEST_TIMEOUT_MILLIS = 5_000L
  }
}

private class FakeLibReferenceSettingsRepository : LibReferenceSettingsRepository {
  override val appListDisplayOptions = 0
  override val threshold = 1
  override var options = 0
  override val showSystemApps = true
  override val colorfulRuleIcon = true
  override val thresholdChanges: Flow<Int> = emptyFlow()
  override val showSystemAppsChanges: Flow<Unit> = emptyFlow()
  override val colorfulRuleIconChanges: Flow<Boolean> = emptyFlow()

  override suspend fun setThreshold(threshold: Int) = Unit
}

private class FakeInstalledAppRepository(
  private val onLoad: () -> Unit = {}
) : InstalledAppRepository {
  override val packageChanges: SharedFlow<PackageChangeState> = MutableSharedFlow()

  override fun getApplicationList(forceUpdate: Boolean): List<PackageInfo> {
    onLoad()
    return emptyList()
  }

  override fun getInstalledPackages(flags: Int): List<PackageInfo> = emptyList()

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
