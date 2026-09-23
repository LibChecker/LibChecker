package com.absinthe.libchecker.utils.manifest

import com.absinthe.libchecker.domain.app.detail.model.StaticLibItem
import com.absinthe.libchecker.utils.IntentFilterUtils
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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
  fun stopsReadingAnUnboundedManifest() {
    var bytesRead = 0
    val input = object : InputStream() {
      override fun read(): Int {
        bytesRead++
        return 0
      }
      override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        bytesRead += length
        check(bytesRead <= ManifestReader.MAX_MANIFEST_BYTES + 1)
        return length
      }
    }
    assertNull(ManifestReader.getBytesFromInputStream(input))
    assertEquals(ManifestReader.MAX_MANIFEST_BYTES + 1, bytesRead)
  }

  @Test
  fun preservesContentAndCallerStreamOwnershipAtTheLimit() {
    for (size in listOf(0, 19, ManifestReader.MAX_MANIFEST_BYTES)) {
      val bytes = ByteArray(size) { it.toByte() }
      var closed = false
      val input = object : ByteArrayInputStream(bytes) {
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int = super.read(buffer, offset, minOf(length, 997))

        override fun close() {
          closed = true
        }
      }
      assertArrayEquals(bytes, ManifestReader.getBytesFromInputStream(input))
      assertFalse(closed)
    }
  }

  @Test
  fun oversizedIntentFilterManifestFailsBeforeAxmlParsing() {
    val apk = archive(ByteArray(ManifestReader.MAX_MANIFEST_BYTES + 1))
    val parse = IntentFilterUtils::class.java.getDeclaredMethod("parseComponentsFromManifest", String::class.java)
    parse.isAccessible = true

    val failure = assertThrows(InvocationTargetException::class.java) {
      parse.invoke(IntentFilterUtils, apk.absolutePath)
    }
    assertTrue(failure.cause is IllegalArgumentException)
  }

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

  @Test
  fun preservesNodeWriteTimingAndTypedValues() {
    for (tag in listOf("manifest", "application", "uses-sdk", "overlay")) {
      val writer = AxmlWriter()
      val root = writer.child(null, "manifest")
      val node = if (tag == "manifest") root else root.child(null, tag)
      node.attribute("a", "first")
      node.attr(null, "b", -1, null, Res_value(Res_value.TYPE_REFERENCE, 0x7f010001, null, null))
      if (node !== root) node.end()
      root.end()
      writer.end()
      val expected = buildMap<String, Any> {
        if (tag == "overlay") put("overlay", true)
        if (tag == "manifest" || tag == "application") put("a", "first")
        put("b", 0x7f010001)
      }
      val bytes = writer.toByteArray()
      val demands = arrayOf("a", "b")
      assertEquals(tag, expected, ManifestReader.getManifestProperties(bytes, demands))
      assertEquals(tag, expected, ManifestReader.getManifestProperties(archive(bytes), demands))
    }
  }

  @Test
  fun preservesRootEndOverwriteAndIgnoresUndemandedAttributes() {
    val writer = AxmlWriter()
    val root = writer.child(null, "manifest")
    root.attribute("a", "root")
    root.child(null, "application").apply {
      attribute("a", "application")
      attribute("ignored", "ignored")
      end()
    }
    root.end()
    writer.end()
    assertEquals(mapOf("a" to "root"), ManifestReader.getManifestProperties(writer.toByteArray(), arrayOf("a")))
  }

  @Test
  fun preservesNullTypedAttributeHandling() {
    val nullValue = Res_value(Res_value.TYPE_NULL, 0, null, null)
    for (tag in listOf("manifest", "application", "uses-sdk", "overlay")) {
      for (nullLast in listOf(false, true)) {
        val writer = AxmlWriter()
        val root = writer.child(null, "manifest")
        val node = if (tag == "manifest") root else root.child(null, tag)
        node.attr(null, "a", -1, null, if (nullLast) Res_value(Res_value.TYPE_STRING, 0, "first", null) else nullValue)
        node.attr(null, "b", -1, null, if (nullLast) nullValue else Res_value(Res_value.TYPE_STRING, 0, "last", null))
        if (node !== root) node.end()
        root.end()
        writer.end()
        val expected = buildMap<String, Any> {
          if (tag == "overlay") put("overlay", true)
          if (nullLast && (tag == "manifest" || tag == "application")) put("a", "first")
          val lastValue = if (nullLast) nullValue.toString() else "last"
          put("b", lastValue)
        }
        assertEquals("$tag nullLast=$nullLast", expected, ManifestReader.getManifestProperties(writer.toByteArray(), arrayOf("a", "b")))
      }
    }
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
