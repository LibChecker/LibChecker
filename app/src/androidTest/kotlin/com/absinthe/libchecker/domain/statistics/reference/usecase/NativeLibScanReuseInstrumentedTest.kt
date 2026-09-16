package com.absinthe.libchecker.domain.statistics.reference.usecase

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.Constants.ARMV8
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.content.DetailContentResolver
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.utils.PackageUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class NativeLibScanReuseInstrumentedTest {
  @Test
  @SdkSuppress(minSdkVersion = 31)
  fun referenceAppsKeepCompanionValidatedLibraries() = runBlocking {
    val directory = "/data/local/tmp/lc-native-reference-${SystemClock.uptimeMillis()}"
    val libraries = mapOf(
      "flutter" to listOf("libapp.so", "libflutter.so"),
      "unity" to listOf("libmain.so", "libunity.so"),
      "unrelated" to listOf("libapp.so", "libmain.so"),
      "missing" to listOf("libexample.so")
    )
    val installed = GlobalContext.get().get<InstalledAppRepository>()
    val repository = object : InstalledAppRepository by installed {
      override fun getPackageInfo(packageName: String, flags: Int, resolveFrozenArchiveInfo: Boolean): PackageInfo = PackageInfo().apply {
        this.packageName = packageName
        applicationInfo = ApplicationInfo().apply { nativeLibraryDir = "$directory/${packageName.substringAfterLast('.')}" }
      }
    }
    val items = libraries.keys.map { LCItem("test.reference.$it", it, "1", 1, 0, 0, false, ARMV8.toShort(), 0, 24, 0) }
    try {
      libraries.forEach { (name, libs) ->
        shell("mkdir -p $directory/$name")
        libs.forEach { shell("touch $directory/$name/$it") }
      }
      val useCase = GetLibReferenceAppsUseCase(repository)
      for ((name, expected) in mapOf("libapp.so" to "flutter", "libflutter.so" to "flutter", "libmain.so" to "unity")) {
        val result = useCase(GetLibReferenceAppsUseCase.Request(items, name, NATIVE, true))
        assertEquals(name, listOf("test.reference.$expected"), result.items.map { it.packageName })
      }
    } finally {
      libraries.forEach { (name, libs) ->
        libs.forEach { shell("rm -f $directory/$name/$it") }
        shell("rmdir $directory/$name")
      }
      shell("rmdir $directory")
    }
  }

  @Test
  fun installedAppsMatchOriginalReader() {
    val targets = GlobalContext.get().get<InstalledAppRepository>().getApplicationList()
    assertTrue(targets.size > 50)
    for (target in targets) {
      assertEquals(target.packageName, BaselineNativeLibReader.getNativeDirLibs(target), PackageUtils.getNativeDirLibs(target))
    }
    InstrumentationRegistry.getInstrumentation().sendStatus(
      0,
      Bundle().apply {
        putString("stream", "NATIVE_EQUIVALENCE ${targets.size} packages\n")
      }
    )
  }

  @Test
  @SdkSuppress(minSdkVersion = 31)
  fun archiveFixturesMatchOriginalReaderAndCancellationPropagates() {
    val directory = "/data/local/tmp/lc-native-reuse-${SystemClock.uptimeMillis()}"
    val fixtures = mapOf(
      "base.apk" to archive("lib/arm64-v8a/libfixture.so", "lib/arm64-v8a/sub/libfixture.so", "lib/armeabi-v7a/libother.so", "assets/libignored.so"),
      "empty.apk" to archive("classes.dex"),
      "assets.apk" to archive("assets/libignored.so"),
      "corrupt.apk" to "corrupt archive".toByteArray(),
      "split_config.arm64-v8a.apk" to archive("lib/arm64-v8a/libsplit.so")
    )
    shell("mkdir -p $directory")
    try {
      for ((name, bytes) in fixtures) {
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        shell("printf '%s' '$encoded' | base64 -d > $directory/$name")
      }
      assertTrue(File("$directory/base.apk").isFile)
      for (base in listOf("base.apk", "empty.apk", "assets.apk", "corrupt.apk", "missing.apk")) {
        for (split in listOf(false, true)) {
          val target = PackageInfo().apply {
            packageName = "test.native"
            applicationInfo = ApplicationInfo().apply {
              sourceDir = "$directory/$base"
              if (split) splitSourceDirs = arrayOf("$directory/split_config.arm64-v8a.apk")
            }
          }
          val expected = BaselineNativeLibReader.getNativeDirLibs(target)
          assertEquals("$base split=$split", expected, PackageUtils.getNativeDirLibs(target))
          if (base == "base.apk") assertEquals(listOfNotNull("libfixture.so", "libsplit.so".takeIf { split }), expected.map { it.name })
          if (base == "empty.apk" && split) assertEquals(listOf("libsplit.so"), expected.map { it.name })
          for (parseElf in listOf(false, true)) {
            assertEquals(
              "$base split=$split explicit ABI parseElf=$parseElf",
              BaselineNativeLibReader.getNativeDirLibs(target, ARMV8, parseElf),
              PackageUtils.getNativeDirLibs(target, ARMV8, parseElf)
            )
          }
        }
      }
      val target = PackageInfo().apply {
        packageName = "test.native"
        applicationInfo = ApplicationInfo().apply { sourceDir = "$directory/base.apk" }
      }
      var checks = 0
      try {
        PackageUtils.getNativeDirLibs(target, checkCancelled = {
          checks++
          if (checks == 3) throw CancellationException("stop archive scan")
        })
        throw AssertionError("Cancellation must propagate")
      } catch (_: CancellationException) {
        assertEquals(3, checks)
      }
    } finally {
      shell("rm -f " + fixtures.keys.joinToString(" ") { "$directory/$it" })
      shell("rmdir $directory")
    }
  }

  @Test
  @SdkSuppress(minSdkVersion = 31)
  fun mergesBaseAndFeatureSplitLibraries() {
    val directory = "/data/local/tmp/lc-native-splits-${SystemClock.uptimeMillis()}"
    val fixtures = mapOf(
      "base.apk" to archive("lib/arm64-v8a/libapp.so"),
      "split_preload.apk" to archive("lib/arm64-v8a/libflutter.so", "lib/armeabi-v7a/libother.so")
    )
    shell("mkdir -p $directory")
    try {
      fixtures.forEach { (name, bytes) ->
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        shell("printf '%s' '$encoded' | base64 -d > $directory/$name")
      }
      val target = PackageInfo().apply {
        packageName = "test.native.splits"
        applicationInfo = ApplicationInfo().apply {
          sourceDir = "$directory/base.apk"
          splitSourceDirs = arrayOf("$directory/split_preload.apk")
        }
      }
      for (parseElf in listOf(false, true)) {
        for (abi in listOf(null, ARMV8)) {
          val result = PackageUtils.getSourceLibs(target, specifiedAbi = abi, parseElf = parseElf)
          assertEquals(setOf("libapp.so", "libflutter.so"), result["arm64-v8a"].orEmpty().map { it.name }.toSet())
          assertEquals(abi == null, "armeabi-v7a" in result)
        }
      }
      assertEquals(setOf("libapp.so", "libflutter.so"), PackageUtils.getNativeDirLibs(target).map { it.name }.toSet())
    } finally {
      shell("rm -f " + fixtures.keys.joinToString(" ") { "$directory/$it" })
      shell("rmdir $directory")
    }
  }

  @Test
  fun installedDingdingDetailIncludesFlutter() = runBlocking {
    val repository = GlobalContext.get().get<InstalledAppRepository>()
    val target = repository.getPackageInfo("com.alibaba.android.rimet")
    org.junit.Assume.assumeNotNull(target)
    val resolver = DetailContentResolver(InstrumentationRegistry.getInstrumentation().targetContext, repository)
    val items = resolver.getNativeLibraries(target!!, null, false, false, ARMV8).itemsByAbi["arm64-v8a"].orEmpty()
    assertTrue(items.any { it.name == "libflutter.so" })
    val chips = resolver.buildChipList(target, null, false, "arm64-v8a", items, false)
    assertEquals("Flutter", chips.single { it.item.name == "libapp.so" }.rule?.label)
  }

  @Test
  fun measureDingdingNativeReaders() {
    val target = GlobalContext.get().get<InstalledAppRepository>().getPackageInfo("com.alibaba.android.rimet")!!
    val readers = linkedMapOf<String, () -> Any>(
      "source-list" to { PackageUtils.getSourceLibs(target, parseElf = false) },
      "source-selected-elf" to { PackageUtils.getSourceLibs(target, parseElf = false, parseElfForAbi = ARMV8) },
      "source-explicit-elf" to { PackageUtils.getSourceLibs(target, specifiedAbi = ARMV8, parseElf = true) },
      "statistics-native-dir" to { PackageUtils.getNativeDirLibs(target) }
    )
    readers.forEach { (name, read) ->
      repeat(3) { read() }
      val times = List(15) {
        val start = SystemClock.elapsedRealtimeNanos()
        read()
        (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
      }.sorted()
      println("NATIVE_TIMING $name median=${times[7]} min=${times.first()} max=${times.last()}")
    }
  }

  private fun archive(vararg entries: String): ByteArray = ByteArrayOutputStream().use { bytes ->
    ZipOutputStream(bytes).use { zip ->
      for (name in entries) {
        zip.putNextEntry(ZipEntry(name))
        zip.write("fixture".toByteArray())
        zip.closeEntry()
      }
    }
    bytes.toByteArray()
  }

  private fun shell(command: String) {
    val descriptors = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommandRw("sh")
    ParcelFileDescriptor.AutoCloseOutputStream(descriptors[1]).use {
      it.write("set -e\n$command\necho LC_FIXTURE_OK\n".toByteArray())
    }
    val output = ParcelFileDescriptor.AutoCloseInputStream(descriptors[0]).use { it.readBytes().decodeToString() }
    assertTrue("Fixture shell command failed: $output", output.trim().endsWith("LC_FIXTURE_OK"))
  }
}
