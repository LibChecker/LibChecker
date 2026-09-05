package com.absinthe.libchecker.domain.statistics.reference.usecase

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.lifecycle.LifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.domain.app.list.model.InstalledPackageState
import com.absinthe.libchecker.domain.app.model.AppInstallSource
import com.absinthe.libchecker.domain.app.model.PackageChangeState
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

  @Test
  fun scanningProgressStartsBeforeBatchLoadAndCountsCompletedTargets() = runBlocking {
    val progress = mutableListOf<Int>()
    val repository = RecordingInstalledAppRepository(
      targets = List(2) { number ->
        PackageInfo().apply {
          packageName = "test.package$number"
          applicationInfo = ApplicationInfo()
        }
      },
      onBatch = { assertEquals(listOf(0), progress) }
    )
    ComputeLibReferenceUseCase(repository).buildIndex(
      ComputeLibReferenceUseCase.ReferenceConfig(true, LibReferenceOptions.SERVICES),
      progress::add
    )
    assertEquals(listOf(0, 50, 100), progress)
  }

  @Test
  fun matchingProgressCountsCompletedReferences() = runBlocking {
    val index = ComputeLibReferenceUseCase.ReferenceIndex(emptyMap()).apply {
      addReference("test.permission.ONE", "test.package", PERMISSION)
      addReference("test.permission.TWO", "test.package", PERMISSION)
    }
    val progress = mutableListOf<Int>()
    val result = ComputeLibReferenceUseCase(RecordingInstalledAppRepository()).matchRules(
      index,
      ComputeLibReferenceUseCase.MatchConfig(threshold = 1, onlyNotMarked = false),
      progress::add
    )
    assertEquals(2, result?.size)
    assertEquals(listOf(0, 50, 100), progress)
  }

  @Test
  fun cancellationDuringBatchStopsRemainingTypesAndProgress() = runBlocking {
    val job = Job()
    var batchCalls = 0
    val progress = mutableListOf<Int>()
    val repository = RecordingInstalledAppRepository(onBatch = {
      batchCalls++
      job.cancel()
    })
    try {
      withContext(job) {
        ComputeLibReferenceUseCase(repository).buildIndex(
          ComputeLibReferenceUseCase.ReferenceConfig(
            true,
            LibReferenceOptions.SERVICES or LibReferenceOptions.ACTIVITIES
          ),
          progress::add
        )
      }
    } catch (_: CancellationException) {
      // The synchronous batch completes, then cancellation must propagate.
    }
    assertEquals(1, batchCalls)
    assertEquals(listOf(0), progress)
  }
}

private class RecordingInstalledAppRepository(
  private val targets: List<PackageInfo> = listOf(PackageInfo().apply { packageName = "test.package" }),
  private val onBatch: () -> Unit = {}
) : InstalledAppRepository {
  private val activeBatchQueries = AtomicInteger()
  private val highestConcurrentBatchQueries = AtomicInteger()

  val maxConcurrentBatchQueries: Int
    get() = highestConcurrentBatchQueries.get()

  override val packageChanges: SharedFlow<PackageChangeState> = MutableSharedFlow()

  override fun getApplicationList(forceUpdate: Boolean): List<PackageInfo> = targets

  override fun getInstalledPackages(flags: Int): List<PackageInfo> {
    onBatch()
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
