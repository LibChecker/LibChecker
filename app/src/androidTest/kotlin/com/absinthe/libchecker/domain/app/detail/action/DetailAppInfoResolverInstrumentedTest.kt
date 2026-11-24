package com.absinthe.libchecker.domain.app.detail.action

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class DetailAppInfoResolverInstrumentedTest {
  @Test
  fun deniedFilePickerProbePreservesOtherHandlersAndMarketAction() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val koin = GlobalContext.get()
    val installedApps = object : InstalledAppRepository by koin.get<InstalledAppRepository>() {
      override fun getPermissions(packageName: String) = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val resolver = DetailAppInfoResolver(context, context.packageName, installedApps, koin.get(), koin.get()) { intent, _ ->
      when {
        intent.action == Intent.ACTION_GET_CONTENT && intent.`package` == "restricted" -> throw SecurityException("check isProfile")
        intent.action == Intent.ACTION_GET_CONTENT -> listOf(handler("files"))
        intent.type == "application/vnd.android.package-archive" -> listOf(handler("restricted"), handler("files"))
        intent.scheme == "market" -> listOf(handler("store"))
        else -> emptyList()
      }
    }
    assertEquals(listOf("files", "store"), resolver.getAppInfoActions(context.packageName).map { it.packageName })
  }

  private fun handler(pkg: String) = ResolveInfo().apply {
    activityInfo = ActivityInfo().apply {
      name = "Handler"
      packageName = pkg
      nonLocalizedLabel = pkg
      applicationInfo = ApplicationInfo().apply { packageName = pkg }
    }
  }
}
