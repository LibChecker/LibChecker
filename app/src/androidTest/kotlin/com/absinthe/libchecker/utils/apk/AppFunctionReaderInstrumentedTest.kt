package com.absinthe.libchecker.utils.apk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.appfunctions.toEvidence
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.model.AppFunctionInfo
import com.absinthe.libchecker.domain.app.model.VersionedFeature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFunctionReaderInstrumentedTest {
  @Test
  fun rejectsExcessiveFunctionCountAndText() {
    val many = (1..129).joinToString("") { "<appfunction><id>f$it</id></appfunction>" }
    val longText = "<appfunction><id>f</id><description>${"x".repeat(4097)}</description></appfunction>"
    for (content in listOf(many, longText)) {
      assertThrows(Exception::class.java) {
        AppFunctionReader.readFunctions("<appfunctions>$content</appfunctions>".toByteArray())
      }
    }
  }

  @Test
  fun functionAndTextBudgetsAreSharedAcrossMetadataFiles() {
    fun xml(start: Int, count: Int, description: String = "") = "<appfunctions>" +
      (start until start + count).joinToString("") {
        "<appfunction><id>f$it</id><description>$description</description></appfunction>"
      } + "</appfunctions>"
    val first = AppFunctionReader.readFunctions(xml(0, 128).toByteArray()).associateBy { it.id }
    assertEquals(128, first.size)
    assertThrows(IllegalArgumentException::class.java) {
      AppFunctionReader.readFunctions(xml(128, 1).toByteArray(), first)
    }
    val descriptions = AppFunctionReader.readFunctions(xml(0, 15, "x".repeat(4096)).toByteArray()).associateBy { it.id }
    assertThrows(IllegalArgumentException::class.java) {
      AppFunctionReader.readFunctions(xml(15, 1, "x".repeat(4096)).toByteArray(), descriptions)
    }
  }

  @Test
  fun appFunctionEvidenceUsesStableName() {
    assertEquals("app_function", VersionedFeature(Features.Ext.APP_FUNCTION).toEvidence().name)
  }

  @Test
  fun readsCurrentAndLegacyIdsButNotNestedParameterIds() {
    val xml = """
      <appfunctions>
        <appfunction><id>sample.search</id><name>Search</name><description>Find &lt;items&gt; &amp; notes.</description>
          <parameters><functionId>wrong</functionId><name>query</name><description>Wrong description</description></parameters>
          <functionId>sample.search</functionId></appfunction>
        <appfunction><function_id>sample.legacy</function_id></appfunction>
        <appfunction><id>sample.runtime</id></appfunction>
        <appfunction><functionId>sample.search</functionId></appfunction>
        <AppFunctionComponentMetadataDocument><functionId>wrong</functionId></AppFunctionComponentMetadataDocument>
      </appfunctions>
    """.trimIndent()
    assertEquals(
      listOf(
        AppFunctionInfo("sample.search", "Search", "Find <items> & notes."),
        AppFunctionInfo("sample.legacy"),
        AppFunctionInfo("sample.runtime")
      ),
      AppFunctionReader.readFunctions(xml.toByteArray())
    )
  }

  @Test
  fun rejectsDoctypeWrongRootAndMalformedMetadata() {
    for (xml in listOf(
      "<!DOCTYPE appfunctions [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><appfunctions/>",
      "<other><appfunction><functionId>wrong</functionId></appfunction></other>",
      "<appfunctions><appfunction>"
    )) {
      assertThrows(xml, Exception::class.java) { AppFunctionReader.readFunctions(xml.toByteArray()) }
    }
    assertTrue(AppFunctionReader.readFunctions("<appfunctions/>".toByteArray()).isEmpty())
  }

  @Test
  fun readsFunctionsFromTheBuiltApkWithoutRegistryPermissions() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val functions = AppFunctionReader.read(info)
    assertEquals(5, functions.size)
    assertTrue(functions.any { it.name == "findApps" })
    assertTrue(functions.all { !it.description.isNullOrBlank() })
  }
}
