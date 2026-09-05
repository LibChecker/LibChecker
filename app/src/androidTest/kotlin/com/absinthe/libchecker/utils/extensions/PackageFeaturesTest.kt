package com.absinthe.libchecker.utils.extensions

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.absinthe.libchecker.compat.IZipFile
import com.absinthe.libchecker.database.entity.Features
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Collections
import java.util.concurrent.CancellationException
import java.util.zip.ZipEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageFeaturesTest {
  @Test
  fun metadataDeterminesBothFeaturesWithoutOpeningDex() {
    val zip = TrackingZip(listOf("META-INF/app.kotlin_module", "META-INF/androidx.compose.ui_ui.version", "classes.dex"))
    val features = packageInfo().getFeatures(zip)
    assertTrue(features and Features.KOTLIN_USED != 0)
    assertTrue(features and Features.JETPACK_COMPOSE != 0)
    assertEquals(0, zip.dexOpens)
  }

  @Test
  fun unresolvedComposeStillScansDexAndPreservesKotlinOnCorruption() {
    val zip = TrackingZip(listOf("kotlin/kotlin.kotlin_builtins", "classes.dex"))
    val features = packageInfo().getFeatures(zip)
    assertTrue(features and Features.KOTLIN_USED != 0)
    assertEquals(0, features and Features.JETPACK_COMPOSE)
    assertEquals(1, zip.dexOpens)
  }

  @Test
  fun cancellationIsNotConvertedToMissingFeatures() {
    assertThrows(CancellationException::class.java) {
      packageInfo().getFeatures(TrackingZip(listOf("classes.dex"))) { throw CancellationException() }
    }
    assertThrows(CancellationException::class.java) {
      packageInfo().getFeatures { throw CancellationException() }
    }
  }

  private fun packageInfo() = PackageInfo().apply {
    applicationInfo = ApplicationInfo().apply { sourceDir = "/missing.apk" }
  }

  private class TrackingZip(names: List<String>) : IZipFile {
    private val entries = names.map(::ZipEntry)
    var dexOpens = 0
    override fun getZipEntries() = Collections.enumeration(entries)
    override fun getEntry(name: String) = entries.find { it.name == name }
    override fun getInputStream(entry: ZipEntry): InputStream {
      if (entry.name.endsWith(".dex")) dexOpens++
      return ByteArrayInputStream(byteArrayOf())
    }
    override fun close() = Unit
  }
}
