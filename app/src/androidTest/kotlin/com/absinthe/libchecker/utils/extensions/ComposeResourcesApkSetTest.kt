package com.absinthe.libchecker.utils.extensions

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.domain.app.buildmetadata.ComposeResourcesDetector.usesComposeMultiplatformResources
import com.absinthe.libchecker.domain.app.packageinfo.GetArchivePackageInfoUseCase
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pxb.android.Res_value
import pxb.android.axml.AxmlWriter

@SdkSuppress(minSdkVersion = 31)
class ComposeResourcesApkSetTest {
  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private val directory = File("/data/local/tmp/lc-cmp-${UUID.randomUUID()}")
  private val packageName = "com.example.cmp"

  @Before
  fun setUp() {
    shell("mkdir -m 777 ${directory.path}")
  }

  @After
  fun tearDown() {
    shell("rm -rf ${directory.path}")
  }

  @Test
  fun apksEvidenceCannotCrossPackageBoundaries() {
    assertArchivePackageIsolation(xapk = false)
  }

  @Test
  fun xapkEvidenceCannotCrossPackageBoundaries() {
    assertArchivePackageIsolation(xapk = true)
  }

  @Test
  fun childAttributesCannotOverrideSplitPackageIdentity() {
    val split = apk("com.example.unrelated", "config.resources", payload = true, applicationPackage = packageName)
    assertFalse(archive(false, split).usesComposeMultiplatformResources())
  }

  private fun assertArchivePackageIsolation(xapk: Boolean) {
    val own = archive(xapk, apk(packageName, "config.resources", payload = true))
    assertTrue("Valid same-package archive", own.usesComposeMultiplatformResources())
    val mixed = archive(xapk, apk("com.example.unrelated", "config.resources", payload = true))
    assertFalse("Mixed-package archive", mixed.usesComposeMultiplatformResources())
  }

  @Test
  fun archiveRequiresMatchingVersionAndUniqueSplitIdentity() {
    for (split in listOf(
      apk(packageName, "config.resources", version = 2, payload = true),
      apk(packageName, "config.resources", majorVersion = 1, payload = true),
      apk(packageName, payload = true)
    )) {
      assertFalse(archive(false, split).usesComposeMultiplatformResources())
    }
    val base = write("base.apk", apk(packageName, provider = true))
    val split = apk(packageName, "config.resources", payload = true)
    val info = packageInfo(base).apply {
      applicationInfo!!.splitSourceDirs = arrayOf(
        write("split_one.apk", split).path,
        write("split_two.apk", split).path
      )
    }
    assertFalse(info.usesComposeMultiplatformResources())
  }

  @Test
  fun frozenPackageRecoversResourceSplitWhenMetadataIsMissing() {
    val base = write("base.apk", apk(packageName, provider = true))
    write("split_config.resources.apk", apk(packageName, "config.resources", payload = true))
    val info = packageInfo(base).apply {
      applicationInfo!!.splitSourceDirs = null
    }
    assertFalse("Active packages use their declared split set", info.usesComposeMultiplatformResources())
    info.applicationInfo!!.enabled = false
    assertTrue(info.usesComposeMultiplatformResources())
    ZipFileCompat(base).use { zip ->
      assertTrue(info.usesComposeMultiplatformResources(zip))
      assertNotNull("Caller-owned ZIP must remain open", zip.getEntry("AndroidManifest.xml"))
    }
    info.applicationInfo!!.splitSourceDirs = arrayOf(File(directory, "missing.apk").path)
    assertFalse("Recovery cannot hide a missing declared split", info.usesComposeMultiplatformResources())
    info.applicationInfo!!.splitSourceDirs = null
    write("split_config.resources.apk", apk("com.example.unrelated", "config.resources", payload = true))
    assertFalse("Recovered splits must also belong to the package", info.usesComposeMultiplatformResources())
  }

  @Test
  fun incompleteOrUnreadableSetCannotProducePositiveResult() {
    val base = write("base.apk", apk(packageName, provider = true, payload = true))
    val info = packageInfo(base).apply {
      applicationInfo!!.splitSourceDirs = arrayOf(File(directory, "missing.apk").path)
    }
    assertFalse(info.usesComposeMultiplatformResources())
    info.applicationInfo!!.splitSourceDirs = arrayOf(write("broken.apk", byteArrayOf(1, 2, 3)).path)
    assertFalse(info.usesComposeMultiplatformResources())
    info.applicationInfo!!.splitSourceDirs = arrayOf(write("no-manifest.apk", zip(mapOf("asset" to byteArrayOf(1)))).path)
    assertFalse(info.usesComposeMultiplatformResources())
  }

  private fun archive(xapk: Boolean, split: ByteArray): PackageInfo {
    val baseName = if (xapk) "$packageName.apk" else "base.apk"
    val splitName = if (xapk) "config.resources.apk" else "split_config.resources.apk"
    val entries = linkedMapOf(
      baseName to apk(packageName, provider = true),
      splitName to split
    )
    if (xapk) {
      entries["manifest.json"] = """
        {
          "xapk_version": 2, "package_name": "$packageName", "name": "CMP fixture",
          "version_code": 1, "version_name": "1", "split_configs": ["config.resources"],
          "split_apks": [
            {"file": "$baseName", "id": "base"},
            {"file": "$splitName", "id": "config.resources"}
          ]
        }
      """.trimIndent().toByteArray()
    }
    val file = write("${UUID.randomUUID()}.${if (xapk) "xapk" else "apks"}", zip(entries))
    return checkNotNull(GetArchivePackageInfoUseCase()(file))
  }

  private fun packageInfo(base: File): PackageInfo {
    return checkNotNull(
      instrumentation.targetContext.packageManager.getPackageArchiveInfo(base.path, PackageManager.GET_PROVIDERS)
    ).apply {
      applicationInfo!!.sourceDir = base.path
      applicationInfo!!.publicSourceDir = base.path
    }
  }

  private fun apk(
    name: String,
    split: String? = null,
    version: Int = 1,
    majorVersion: Int = 0,
    provider: Boolean = false,
    payload: Boolean = false,
    applicationPackage: String? = null
  ): ByteArray {
    val android = "http://schemas.android.com/apk/res/android"
    val writer = AxmlWriter()
    writer.ns("android", android, 1)
    val root = writer.child(null, "manifest")
    root.attr(null, "package", -1, name, Res_value.newStringValue(name))
    root.attr(android, "versionCode", 0x0101021b, null, Res_value(Res_value.TYPE_INT_DEC, version, null, null))
    root.attr(android, "versionCodeMajor", 0x01010576, null, Res_value(Res_value.TYPE_INT_DEC, majorVersion, null, null))
    if (split != null) root.attr(null, "split", -1, split, Res_value.newStringValue(split))
    root.child(null, "uses-sdk").apply {
      attr(android, "minSdkVersion", 0x0101020c, null, Res_value(Res_value.TYPE_INT_DEC, 24, null, null))
      attr(android, "targetSdkVersion", 0x01010270, null, Res_value(Res_value.TYPE_INT_DEC, 36, null, null))
      end()
    }
    root.child(null, "application").apply {
      if (applicationPackage != null) {
        attr(null, "package", -1, applicationPackage, Res_value.newStringValue(applicationPackage))
      }
      if (provider) {
        child(null, "provider").apply {
          attr(android, "name", 0x01010003, null, Res_value(Res_value.TYPE_STRING, 0, "org.jetbrains.compose.resources.AndroidContextProvider", null))
          attr(android, "authorities", 0x01010018, null, Res_value(Res_value.TYPE_STRING, 0, "$name.resources", null))
          attr(android, "exported", 0x01010010, null, Res_value(Res_value.TYPE_INT_BOOLEAN, 0, null, null))
          end()
        }
      }
      end()
    }
    root.end()
    writer.end()
    return zip(
      buildMap {
        put("AndroidManifest.xml", writer.toByteArray())
        if (payload) put("assets/composeResources/example/files/config.json", "{}".toByteArray())
      }
    )
  }

  private fun zip(entries: Map<String, ByteArray>): ByteArray {
    return ByteArrayOutputStream().apply {
      ZipOutputStream(this).use { output ->
        entries.forEach { (name, bytes) ->
          output.putNextEntry(ZipEntry(name))
          output.write(bytes)
          output.closeEntry()
        }
      }
    }.toByteArray()
  }

  private fun write(name: String, bytes: ByteArray): File {
    val file = File(directory, name)
    val pipes = instrumentation.uiAutomation.executeShellCommandRw("tee ${file.path}")
    ParcelFileDescriptor.AutoCloseOutputStream(pipes[1]).use { it.write(bytes) }
    ParcelFileDescriptor.AutoCloseInputStream(pipes[0]).use { it.readBytes() }
    shell("chmod 644 ${file.path}")
    check(file.length() == bytes.size.toLong()) { "Fixture write failed: ${file.path}" }
    return file
  }

  private fun shell(command: String) {
    ParcelFileDescriptor.AutoCloseInputStream(
      instrumentation.uiAutomation.executeShellCommand(command)
    ).use { it.readBytes() }
  }
}
