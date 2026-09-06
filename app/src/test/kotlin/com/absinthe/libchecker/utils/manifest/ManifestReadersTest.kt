package com.absinthe.libchecker.utils.manifest

import com.absinthe.libchecker.domain.app.detail.model.StaticLibItem
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import pxb.android.Res_value
import pxb.android.axml.AxmlWriter
import pxb.android.axml.NodeVisitor

class ManifestReadersTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun readsAllFiveViewsOfTheSameManifest() {
    val writer = AxmlWriter()
    val manifest = writer.child(null, "manifest")
    manifest.attribute("package", "sample.app")
    manifest.child(null, "uses-sdk").apply {
      attribute("minSdkVersion", 24)
      end()
    }
    manifest.child(null, "uses-permission").apply {
      attribute("name", "sample.permission")
      attribute("maxSdkVersion", 30)
      end()
    }
    manifest.child(null, "application").apply {
      attribute("label", "Sample")
      child(null, "service").apply {
        attribute("name", ".Service")
        end()
      }
      child(null, "uses-static-library").apply {
        attribute("name", "sample.library")
        attribute("version", 3)
        attribute("certDigest", "digest")
        end()
      }
      end()
    }
    manifest.end()
    writer.end()
    val bytes = writer.toByteArray()
    val apk = archive(bytes)
    val demands = arrayOf("package", "minSdkVersion")
    assertEquals(mapOf("package" to "sample.app", "minSdkVersion" to "24"), ManifestReader.getManifestProperties(bytes, demands))
    assertEquals(ManifestReader.getManifestProperties(bytes, demands), ManifestReader.getManifestProperties(apk, demands))
    assertEquals(mapOf("label" to "Sample"), ApplicationReader.getManifestProperties(bytes))
    assertEquals(ApplicationReader.getManifestProperties(bytes), ApplicationReader.getManifestProperties(apk))
    val full = FullManifestReader(bytes, null)
    assertEquals("sample.app", full.properties["package"])
    val fullFile = FullManifestReader(apk, null)
    assertEquals(full.properties, fullFile.properties)
    assertEquals(full.permissionList, fullFile.permissionList)
    assertEquals(full.services, fullFile.services)
    assertEquals(listOf("sample.permission"), full.permissionList)
    assertEquals(listOf(".Service"), full.services)
    assertEquals(mapOf("sample.permission" to 30), HiddenPermissionsReader.getHiddenPermissions(apk))
    assertEquals(mapOf("sample.library" to StaticLibItem("sample.library", 3, "digest")), StaticLibraryReader.getStaticLibrary(apk))
  }

  @Test
  fun missingAndMalformedManifestsKeepEmptyResults() {
    for (apk in listOf(archive(null), archive(byteArrayOf(1, 2, 3)))) {
      assertTrue(ManifestReader.getManifestProperties(apk, arrayOf("package")).isEmpty())
      assertTrue(ApplicationReader.getManifestProperties(apk).isEmpty())
      assertTrue(FullManifestReader(apk, null).properties.isEmpty())
      assertTrue(HiddenPermissionsReader.getHiddenPermissions(apk).isEmpty())
      assertTrue(StaticLibraryReader.getStaticLibrary(apk).isEmpty())
    }
    assertTrue(ManifestReader.getManifestProperties(byteArrayOf(1, 2, 3), arrayOf("package")).isEmpty())
    assertTrue(ApplicationReader.getManifestProperties(byteArrayOf(1, 2, 3)).isEmpty())
    assertTrue(FullManifestReader(byteArrayOf(1, 2, 3), null).properties.isEmpty())
  }

  private fun archive(manifest: ByteArray?) = temporaryFolder.newFile().apply {
    ZipOutputStream(outputStream()).use { zip ->
      zip.putNextEntry(ZipEntry(if (manifest == null) "other" else "AndroidManifest.xml"))
      zip.write(manifest ?: byteArrayOf())
      zip.closeEntry()
    }
  }

  private fun NodeVisitor.attribute(name: String, value: Any) {
    val typedValue = if (value is Int) {
      Res_value(Res_value.TYPE_INT_DEC, value, null, null)
    } else {
      Res_value(Res_value.TYPE_STRING, 0, value.toString(), null)
    }
    attr(null, name, -1, null, typedValue)
  }
}
