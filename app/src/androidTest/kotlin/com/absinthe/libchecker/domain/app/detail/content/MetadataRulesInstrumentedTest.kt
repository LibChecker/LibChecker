package com.absinthe.libchecker.domain.app.detail.content

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.api.bean.LibDetailBean
import com.absinthe.libchecker.domain.app.detail.action.DetailItemResolver
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.app.repository.LibraryDetailRepository
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import com.absinthe.rulesbundle.Rule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class MetadataRulesInstrumentedTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private val installed = GlobalContext.get().get<InstalledAppRepository>()
  private val rule = Rule("example.policy", METADATA, "Example library", -1, "", null, false)

  @Test
  fun installedAndManifestOnlyMetadataUseTheSameRuleLookup(): Unit = runBlocking {
    val lookups = mutableListOf<String>()
    val resolver = DetailContentResolver(context, installed) { key ->
      lookups.add(key)
      rule.takeIf { key == it.libName }
    }
    val info = PackageInfo().apply {
      applicationInfo = ApplicationInfo().apply {
        metaData = Bundle().apply {
          putString("example.policy", "policy-value")
          putString("example.unknown", "unknown-value")
        }
      }
    }
    val preview = ApkPreviewInfo(
      "example.app", 1, "1", 35, 35, 24, 0, emptySet(), emptyMap(), emptyMap(),
      emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
      mapOf("example.policy" to "policy-value", "example.unknown" to "unknown-value")
    )
    val results = listOf(resolver.getMetadataChips(info, null, false), resolver.getMetadataChips(PackageInfo(), preview, true))
    results.forEach { items ->
      assertEquals(rule, items.single { it.item.name == "example.policy" }.rule)
      assertEquals("policy-value", items.single { it.item.name == "example.policy" }.item.source)
      assertNull(items.single { it.item.name == "example.unknown" }.rule)
    }
    assertEquals(2, lookups.count { it == "example.policy" })
    assertEquals(2, lookups.count { it == "example.unknown" })
  }

  @Test
  fun metadataDetailsUseExistingRepositoryForExactAndRegexPaths(): Unit = runBlocking {
    val requests = mutableListOf<Pair<String, String>>()
    val repository = object : LibraryDetailRepository {
      override suspend fun requestLibraryDetail(categoryDir: String, libPath: String): LibDetailBean {
        requests.add(categoryDir to libPath)
        return LibDetailBean(
          listOf(LibDetailBean.Data("en", LibDetailBean.DataBean("Example library", "Example team", emptyList(), "Description", "https://example.com"))),
          "example-library"
        )
      }
      override suspend fun getRepoPushedAt(owner: String, repo: String): String? = null
    }
    val resolver = DetailItemResolver(context.packageManager, installed, repository)
    for (regexName in listOf(null, "example.*")) {
      val result = resolver(DetailItemResolver.Request("example.policy", METADATA, regexName, true, "en"))
      assertTrue(result is DetailItemResolver.Result.Found)
    }
    assertEquals(listOf("metadata-libs" to "example.policy", "metadata-libs/regex" to "example.*"), requests)
    assertEquals(DetailItemResolver.Result.NotFound, resolver(DetailItemResolver.Request("unknown", METADATA, null, false, "en")))
    assertEquals(2, requests.size)
  }
}
