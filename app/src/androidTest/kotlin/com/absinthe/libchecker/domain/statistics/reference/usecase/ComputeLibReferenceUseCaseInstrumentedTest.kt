package com.absinthe.libchecker.domain.statistics.reference.usecase

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.lifecycle.LifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.domain.app.list.model.InstalledPackageState
import com.absinthe.libchecker.domain.app.model.AppInstallSource
import com.absinthe.libchecker.domain.app.model.PackageChangeState
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComputeLibReferenceUseCaseInstrumentedTest {

  @Test
  fun componentPackageBatchesAreLoadedSequentially() = runBlocking(Dispatchers.Default) {
    val repository = RecordingInstalledAppRepository()
    val options = LibReferenceOptions.SERVICES or
      LibReferenceOptions.ACTIVITIES or
      LibReferenceOptions.RECEIVERS or
      LibReferenceOptions.PROVIDERS

    ComputeLibReferenceUseCase(repository).buildIndex(
      ComputeLibReferenceUseCase.ReferenceConfig(showSystemApps = true, options = options),
      onProgress = {}
    )

    assertEquals(1, repository.maxConcurrentBatchQueries)
  }
}

private class RecordingInstalledAppRepository : InstalledAppRepository {
  private val activeBatchQueries = AtomicInteger()
  private val highestConcurrentBatchQueries = AtomicInteger()

  val maxConcurrentBatchQueries: Int
    get() = highestConcurrentBatchQueries.get()

  override val packageChanges: SharedFlow<PackageChangeState> = MutableSharedFlow()

  override fun getApplicationList(forceUpdate: Boolean): List<PackageInfo> = emptyList()

  override fun getInstalledPackages(flags: Int): List<PackageInfo> {
    val activeQueries = activeBatchQueries.incrementAndGet()
    highestConcurrentBatchQueries.updateAndGet { maxOf(it, activeQueries) }
    return try {
      Thread.sleep(BATCH_QUERY_DELAY_MILLIS)
      emptyList()
    } finally {
      activeBatchQueries.decrementAndGet()
    }
  }

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

  private companion object {
    const val BATCH_QUERY_DELAY_MILLIS = 100L
  }
}
