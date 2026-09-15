package com.absinthe.libchecker.domain.app.packageinfo

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class PrepareApkAnalysisPackageInstrumentedTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  @Test
  fun oldCancellationCannotDeleteNewImport() = runBlocking {
    val cache = File(context.cacheDir, "apk-import-test-${UUID.randomUUID()}").apply { mkdir() }
    val oldClosed = CountDownLatch(1)
    val releaseOld = CountDownLatch(1)
    val useCase = useCase { uri ->
      if (uri.lastPathSegment == "old") {
        oldClosed.countDown()
        check(releaseOld.await(30, TimeUnit.SECONDS))
      }
    }
    val old = launch(Dispatchers.Default) { useCase(cache, uri("old")) }
    try {
      assertTrue(oldClosed.await(30, TimeUnit.SECONDS))
      old.cancel()
      val current = useCase(cache, uri("new")) as PrepareApkAnalysisPackageUseCase.Result.Available
      releaseOld.countDown()
      old.join()
      assertTrue(old.isCancelled)
      assertTrue("The old request deleted the new APK", current.file.isFile)
      assertEquals(File(context.applicationInfo.sourceDir).length(), current.file.length())
      assertEquals(listOf(current.file), cache.walkTopDown().filter { it.isFile }.toList())
      current.file.deleteApkAnalysisCache()
      assertTrue(cache.listFiles()!!.isEmpty())
      assertTrue("Cleanup must retain the shared cache directory", cache.isDirectory)
    } finally {
      releaseOld.countDown()
      old.cancelAndJoin()
      cache.deleteRecursively()
    }
  }

  @Test
  fun failedCloseCleansOnlyItsOwnImport() = runBlocking {
    val cache = File(context.cacheDir, "apk-import-test-${UUID.randomUUID()}").apply { mkdir() }
    val useCase = useCase { uri ->
      if (uri.lastPathSegment == "broken") throw IOException("Provider close failed")
    }
    try {
      val current = useCase(cache, uri("good")) as PrepareApkAnalysisPackageUseCase.Result.Available
      assertEquals(PrepareApkAnalysisPackageUseCase.Result.Unreadable, useCase(cache, uri("broken")))
      assertTrue(current.file.isFile)
      assertEquals(listOf(current.file.parentFile), cache.listFiles()!!.toList())
      current.file.deleteApkAnalysisCache()
      assertFalse(current.file.exists())
      assertTrue(cache.listFiles()!!.isEmpty())
    } finally {
      cache.deleteRecursively()
    }
  }

  private fun useCase(onClose: (Uri) -> Unit): PrepareApkAnalysisPackageUseCase {
    val provider = object : ContentProvider() {
      override fun onCreate() = true
      override fun getType(uri: Uri) = "application/vnd.android.package-archive"
      override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
      ): Cursor? = null
      override fun insert(uri: Uri, values: ContentValues?): Uri? = null
      override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
      override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
      override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor {
        val descriptor = ParcelFileDescriptor.open(
          File(this@PrepareApkAnalysisPackageInstrumentedTest.context.applicationInfo.sourceDir),
          ParcelFileDescriptor.MODE_READ_ONLY
        )
        return object : AssetFileDescriptor(descriptor, 0, UNKNOWN_LENGTH) {
          override fun createInputStream(): FileInputStream = object : AutoCloseInputStream(this) {
            override fun close() {
              super.close()
              onClose(uri)
            }
          }
        }
      }
    }
    provider.attachInfo(context, ProviderInfo().apply { authority = "test.apk.import" })
    return PrepareApkAnalysisPackageUseCase(ContentResolver.wrap(provider), GetArchivePackageInfoUseCase())
  }

  private fun uri(name: String) = Uri.parse("content://test.apk.import/$name")
}
