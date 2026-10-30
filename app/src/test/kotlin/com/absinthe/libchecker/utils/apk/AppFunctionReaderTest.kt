package com.absinthe.libchecker.utils.apk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import pxb.android.Res_value
import pxb.android.axml.AxmlWriter
import pxb.android.axml.NodeVisitor

class AppFunctionReaderTest {
  @Test
  fun decompressionBudgetIsSharedAcrossFiles() {
    val budget = AppFunctionReader.ReadBudget()
    val chunk = ByteArray(1024 * 1024)
    repeat(8) { assertEquals(chunk.size, budget.read(chunk.inputStream()).size) }
    assertThrows(IllegalArgumentException::class.java) { budget.read(byteArrayOf(1).inputStream()) }
  }

  @Test
  fun rejectsEntryAtTheFirstBytePastItsLimit() {
    val budget = AppFunctionReader.ReadBudget()
    assertEquals(4, budget.read(ByteArray(4).inputStream(), 4).size)
    assertThrows(IllegalArgumentException::class.java) { budget.read(ByteArray(5).inputStream(), 4) }
  }

  @Test
  fun readsApplicationAndServiceAssetsWithTheAndroidNamespace() {
    val writer = AxmlWriter()
    writer.ns("android", ANDROID, 1)
    val root = writer.child(null, "manifest")
    val app = root.child(null, "application")
    app.property("android.app.appfunctions.v2", "runtime.xml, second.xml")
    val service = app.child(null, "service")
    service.property("android.app.appfunctions.v2", "custom.xml")
    service.property("android.app.appfunctions", "legacy.xml")
    service.property("other", "unrelated.xml")
    service.property("android.app.appfunctions.v2", "../outside.xml")
    service.property("android.app.appfunctions.v2", "spoof.xml", "urn:other")
    service.end()
    app.end()
    root.end()
    writer.end()
    assertEquals(listOf("runtime.xml", "second.xml", "custom.xml", "legacy.xml"), AppFunctionReader.readAssetNames(writer.toByteArray()))
  }

  private fun NodeVisitor.property(name: String, asset: String, namespace: String = ANDROID) {
    child(null, "property").apply {
      attr(namespace, "name", 0x01010003, null, Res_value(Res_value.TYPE_STRING, 0, name, null))
      attr(namespace, "value", 0x01010024, null, Res_value(Res_value.TYPE_STRING, 0, asset, null))
      end()
    }
  }

  private companion object {
    const val ANDROID = "http://schemas.android.com/apk/res/android"
  }
}
