package com.absinthe.libchecker.domain.app.detail.content

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants.ERROR
import com.absinthe.libchecker.data.statistics.AndroidStatisticEvidenceRepository
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.action.ExtractNativeLibraryUseCase
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotEmptyView
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDexClassQuery
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticStringOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticStringPattern
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticArtifactQuery
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildDetailedAbiChartDataUseCase
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.view.app.RingDotsView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
class MissingPackageMetadataInstrumentedTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private val installed = GlobalContext.get().get<InstalledAppRepository>()

  @Test
  fun missingInstalledPackageReturnsNoSignaturesWithoutReportingAnException() = runBlocking {
    var lookups = 0
    val repository = object : InstalledAppRepository by installed {
      override fun getPackageInfo(packageName: String, flags: Int, resolveFrozenArchiveInfo: Boolean): PackageInfo? {
        lookups++
        return null
      }
    }
    val errors = mutableListOf<Throwable>()
    val tree = errorTree(errors)
    Timber.plant(tree)
    try {
      val info = PackageInfo().apply { packageName = "test.missing.package" }
      assertTrue(DetailContentResolver(context, repository).getSignatureChips(info, false).isEmpty())
      assertEquals(1, lookups)
      assertTrue("Missing packages must not be reported as exceptions: $errors", errors.isEmpty())
      val actual = checkNotNull(installed.getPackageInfo(context.packageName))
      assertTrue(DetailContentResolver(context, installed).getSignatureChips(actual, false).isNotEmpty())
    } finally {
      Timber.uproot(tree)
    }
  }

  @Test
  fun missingArchiveReturnsNoSignaturesWithoutReportingAnException() = runBlocking {
    val errors = mutableListOf<Throwable>()
    val tree = errorTree(errors)
    Timber.plant(tree)
    try {
      val resolver = DetailContentResolver(context, installed)
      val missingArchives = listOf(
        PackageInfo(),
        PackageInfo().apply { applicationInfo = ApplicationInfo() },
        PackageInfo().apply {
          applicationInfo = ApplicationInfo().apply { sourceDir = "/data/local/tmp/libchecker-missing-signature.apk" }
        }
      )
      missingArchives.forEach { assertTrue(resolver.getSignatureChips(it, true).isEmpty()) }
      assertTrue("Missing archives must not be reported as exceptions: $errors", errors.isEmpty())
      val actual = checkNotNull(installed.getPackageInfo(context.packageName))
      assertTrue(resolver.getSignatureChips(actual, true).isNotEmpty())
    } finally {
      Timber.uproot(tree)
    }
  }

  @Test
  fun detailedAbiSkipsMissingPackageApplicationAndSourceWithoutReportingExceptions() = runBlocking {
    var lookups = 0
    val repository = object : InstalledAppRepository by installed {
      override fun getPackageInfo(packageName: String, flags: Int, resolveFrozenArchiveInfo: Boolean): PackageInfo? {
        lookups++
        return when (packageName) {
          "missing" -> null
          "application" -> PackageInfo()
          else -> PackageInfo().apply { applicationInfo = ApplicationInfo() }
        }
      }
    }
    val errors = mutableListOf<Throwable>()
    val tree = errorTree(errors)
    Timber.plant(tree)
    try {
      val items = listOf("missing", "application", "source").map {
        LCItem(it, it, "1", 1, 0, 0, false, 0, 0, 0, 0)
      }
      val result = BuildDetailedAbiChartDataUseCase(context, repository)(
        BuildDetailedAbiChartDataUseCase.Request(items, showSystemApps = true)
      ) {}
      assertEquals(3, lookups)
      assertTrue(checkNotNull(result).groups.isEmpty())
      assertTrue("Missing paths must not be reported as exceptions: $errors", errors.isEmpty())
    } finally {
      Timber.uproot(tree)
    }
  }

  @Test
  fun truncatedDexEntrySkipsCorruptedDexWithoutReportingErrorAndContinuesScanning() = runBlocking {
    val corruptedApk = File(context.cacheDir, "truncated-dex-test.apk")
    ZipOutputStream(corruptedApk.outputStream()).use { zip ->
      zip.putNextEntry(ZipEntry("classes.dex"))
      zip.write(byteArrayOf(0x64, 0x65, 0x78))
      zip.closeEntry()
    }
    val errors = mutableListOf<Throwable>()
    val tree = errorTree(errors)
    Timber.plant(tree)
    try {
      val repository = AndroidStatisticEvidenceRepository(context.packageManager, installed)
      val query = StatisticArtifactQuery.DexClasses(
        listOf(
          StatisticDexClassQuery(
            name = StatisticStringPattern(
              operator = StatisticStringOperator.EQUAL,
              value = "Lcom/absinthe/libchecker/data/statistics/AndroidStatisticEvidenceRepository;"
            )
          )
        )
      )
      val packageInfo = PackageInfo().apply {
        packageName = context.packageName
        applicationInfo = ApplicationInfo().apply {
          sourceDir = corruptedApk.absolutePath
          splitSourceDirs = arrayOf(context.applicationInfo.sourceDir)
        }
      }
      val matched = repository.matchesAll(packageInfo, setOf(query))
      assertEquals(mapOf(query to true), matched)
      assertTrue("Truncated DEX entry must not be reported as an error: $errors", errors.isEmpty())
    } finally {
      Timber.uproot(tree)
      corruptedApk.delete()
    }
  }

  @Test
  fun unreadableApkInGetAbiSetReturnsErrorWithoutReportingException() {
    val unreadableApk = File(context.cacheDir, "unreadable-abi-test.apk").apply {
      writeBytes("unreadable".toByteArray())
      setReadable(false)
    }
    val errors = mutableListOf<Throwable>()
    val tree = errorTree(errors)
    Timber.plant(tree)
    try {
      assertEquals(setOf(ERROR), PackageUtils.getAbiSet(unreadableApk, PackageInfo(), isApk = false))
      assertTrue("Unreadable APKs must not be reported as Crashlytics errors: $errors", errors.isEmpty())
    } finally {
      Timber.uproot(tree)
      unreadableApk.setReadable(true)
      unreadableApk.delete()
    }
  }

  @Test
  fun ringDotsViewBoundsDefaultIconMissesAndRendersOwnedHighlightBitmap(): Unit = runBlocking {
    val view = RingDotsView(ContextThemeWrapper(context, R.style.AppTheme))
    val defaultIcon = context.packageManager.defaultActivityIcon
    var calls = 0
    val missedResult = withTimeout(2_000) {
      view.loadAppHighlightBitmap {
        calls++
        defaultIcon
      }
    }
    assertEquals(null, missedResult)
    assertEquals(8, calls)

    val bitmap = withTimeout(2_000) {
      view.loadAppHighlightBitmap { ColorDrawable(Color.RED) }
    }
    assertNotNull(bitmap)
    checkNotNull(bitmap).recycle()
  }

  @Test
  fun writeElfToDownloadsFallsBackWhenPublicDirectoryCannotBeCreated() {
    val useCase = ExtractNativeLibraryUseCase(context, context.packageName)
    val expected = byteArrayOf(0x7f, 0x45, 0x4c, 0x46)
    val target = useCase.writeElfToDownloads(
      dir = "test-elf-fallback",
      elfFileName = "libsample.so",
      inputStream = ByteArrayInputStream(expected),
      downloadsDir = File("/proc/unwritable-downloads")
    )
    try {
      assertTrue(target.isFile)
      assertEquals(expected.toList(), target.readBytes().toList())
    } finally {
      target.delete()
      target.parentFile?.delete()
    }
  }

  @Test
  fun staggeredGridHeaderUsesFullSpanAndStateViewDisablesBeforeRowInsertion() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val stateEnabledDuringInsert = AtomicBoolean(true)
    val committed = CountDownLatch(1)
    lateinit var adapter: BaseQuickAdapter<String, BaseViewHolder>
    instrumentation.runOnMainSync {
      val themed = ContextThemeWrapper(context, R.style.AppTheme)
      val diff = object : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
      }
      adapter = object : BaseQuickAdapter<String, BaseViewHolder>(0, diff) {
        override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int) = createBaseViewHolder(View(parent.context))
        override fun convert(holder: BaseViewHolder, item: String) = Unit
      }
      val header = View(themed).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          40
        )
      }
      adapter.setHeaderView(header)
      adapter.stateView = SnapshotEmptyView(themed)
      adapter.isStateViewEnable = true

      val list = RecyclerView(themed).apply {
        layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        this.adapter = adapter
        measure(
          View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY)
        )
        layout(0, 0, 400, 600)
      }
      assertEquals(2, list.childCount)
      val headerLp = header.layoutParams as? StaggeredGridLayoutManager.LayoutParams
      assertNotNull("Header must use StaggeredGridLayoutManager.LayoutParams", headerLp)
      assertTrue("Header must span all StaggeredGrid columns", checkNotNull(headerLp).isFullSpan)

      adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
          if (itemCount > 0 && adapter.data.isNotEmpty()) {
            stateEnabledDuringInsert.set(adapter.isStateViewEnable)
          }
        }
      })
      adapter.setDiffNewData(mutableListOf("a", "b")) {
        committed.countDown()
      }
    }
    assertTrue("Async diff must commit", committed.await(5, TimeUnit.SECONDS))
    instrumentation.runOnMainSync {
      assertFalse("Empty state view must be disabled before inserting rows", stateEnabledDuringInsert.get())
      assertTrue(adapter.isStateViewEnable)
    }
  }

  private fun errorTree(errors: MutableList<Throwable>) = object : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
      if (priority >= Log.ERROR && t != null) errors.add(t)
    }
  }
}
