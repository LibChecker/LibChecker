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
import com.absinthe.libchecker.constant.Constants.ARMV8
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.utils.PackageUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class NativeLibScanReuseInstrumentedTest {
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
          if (base == "base.apk") assertEquals(listOf("libfixture.so"), expected.map { it.name })
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
