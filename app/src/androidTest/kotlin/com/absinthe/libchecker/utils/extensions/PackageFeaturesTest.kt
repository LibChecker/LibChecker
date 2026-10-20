package com.absinthe.libchecker.utils.extensions

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ProviderInfo
import com.absinthe.libchecker.compat.IZipFile
import com.absinthe.libchecker.database.entity.Features
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Collections
import java.util.concurrent.CancellationException
import java.util.zip.ZipEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageFeaturesTest {
  @Test
  fun metadataDeterminesBothFeaturesInSingleEntryEnumerationWithoutOpeningDex() {
    val zip = TrackingZip(listOf("META-INF/app.kotlin_module", "META-INF/androidx.compose.ui_ui.version", "classes.dex"))
    val features = packageInfo().getFeatures(zip)
    assertTrue(features and Features.KOTLIN_USED != 0)
    assertTrue(features and Features.JETPACK_COMPOSE != 0)
    assertEquals(0, zip.dexOpens)
    assertEquals(1, zip.entryEnumerations)
  }

  @Test
  fun composeResourcesRequiresPayloadAndOwnRuntimeEvidenceAcrossApks() {
    val baseZip = TrackingZip(listOf("classes.dex"))
    val resourceSplitZip = TrackingZip(listOf("assets/composeResources/example.generated.resources/files/config.json"))
    val piWithProvider = packageInfo().apply {
      packageName = "com.example.resources"
      providers = arrayOf(
        ProviderInfo().apply {
          name = "org.jetbrains.compose.resources.AndroidContextProvider"
        }
      )
    }
    assertTrue(piWithProvider.hasComposeMultiplatformResources(listOf(baseZip, resourceSplitZip)))
    assertEquals(0, baseZip.dexOpens)
    assertEquals(0, resourceSplitZip.dexOpens)

    assertFalse(piWithProvider.hasComposeMultiplatformResources(listOf(baseZip)))
    assertFalse(
      piWithProvider.hasComposeMultiplatformResources(
        listOf(TrackingZip(listOf("assets/composeResources/", "assets/composeResources/example/")))
      )
    )
    assertFalse(
      piWithProvider.hasComposeMultiplatformResources(
        listOf(TrackingZip(listOf("assets/composeresources/example/files/config.json")))
      )
    )

    val piWithoutProvider = packageInfo().apply {
      packageName = "com.example.no-provider"
      providers = null
    }
    assertFalse(piWithoutProvider.hasComposeMultiplatformResources(listOf(resourceSplitZip)))
  }

  @Test
  fun unresolvedComposeStillScansDexAndPreservesKotlinOnCorruption() {
    val zip = TrackingZip(listOf("kotlin/kotlin.kotlin_builtins", "classes.dex"))
    val features = packageInfo().getFeatures(zip)
    assertTrue(features and Features.KOTLIN_USED != 0)
    assertEquals(0, features and Features.JETPACK_COMPOSE)
    assertEquals(1, zip.dexOpens)
    assertEquals(2, zip.entryEnumerations)
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
    var entryEnumerations = 0
    override fun getZipEntries() = Collections.enumeration(entries).also { entryEnumerations++ }
    override fun getEntry(name: String) = entries.find { it.name == name }
    override fun getInputStream(entry: ZipEntry): InputStream {
      if (entry.name.endsWith(".dex")) dexOpens++
      return ByteArrayInputStream(byteArrayOf())
    }
    override fun close() = Unit
  }
}
