package com.absinthe.libchecker.data.rules

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.api.request.CloudRuleBundleRequest
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.rulesbundle.RuleReader
import java.io.File
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.HttpException
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class InstalledRulesBundleTest {
  @Test fun missingManifestDoesNotFallBackOrChangeInstalledVersion() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val version = RulesRepository.getLocalVersion(context)
    val request = Proxy.newProxyInstance(
      CloudRuleBundleRequest::class.java.classLoader,
      arrayOf(CloudRuleBundleRequest::class.java)
    ) { _, method, _ ->
      check(method.name == "requestV5Manifest")
      throw HttpException(Response.error<Any>(404, "missing".toResponseBody()))
    } as CloudRuleBundleRequest
    val repository = AndroidCloudRulesRepository(context, request)
    val failure = runCatching { repository.getVersionInfo() }.exceptionOrNull()
    assertEquals(404, (failure as HttpException).code())
    assertTrue(runCatching { repository.getDownloadRequest() }.isFailure)
    assertEquals(version, RulesRepository.getLocalVersion(context))
  }

  @Test fun bundledDatabaseUsesDrawableAndCloudDetails() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    RulesRepository.init(context)
    assertEquals(setOf("rules.db", "metadata.json"), context.assets.list("lcrules/v5")!!.toSet())
    assertTrue(context.assets.list("rules").isNullOrEmpty())
    val rule = requireNotNull(RulesRepository.getRule("libflutter.so", NATIVE, true))
    assertEquals("Flutter", rule.label)
    assertTrue(rule.descriptionUrl?.endsWith("native-libs/libflutter.so.json") == true)
    assertNotNull(context.getDrawable(rule.iconRes))
    val database = RulesRepository.getDatabaseFile(context)
    assertTrue(!database.canonicalPath.startsWith(File(context.noBackupFilesDir, "rules-v5").canonicalPath + "/"))
    val directory = database.parentFile!!
    assertTrue(!File(directory, "details").exists())
    assertTrue(!File(directory, "icons").exists())
    RuleReader.open(RulesRepository.getDatabaseFile(context)).use { reader ->
      assertEquals(5, reader.metadata.schemaVersion)
      assertEquals(RulesRepository.getLocalVersion(context).toLong(), reader.metadata.dataVersion)
      assertTrue(reader.metadata.ruleCount >= 2832)
    }
  }
}
