package com.absinthe.libchecker.data.statistics

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import java.lang.reflect.Proxy
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AndroidKotlinVersionRepositoryTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()
  private var componentReads = 0
  private val info = PackageInfo().apply { packageName = "com.example.app" }
  private val installedApps = Proxy.newProxyInstance(
    InstalledAppRepository::class.java.classLoader,
    arrayOf(InstalledAppRepository::class.java)
  ) { _, method, args ->
    check(method.name == "getPackageInfo")
    if (args[1] != 0) componentReads++
    info
  } as InstalledAppRepository

  @Test
  fun cachesUnknownAndInvalidatesOnPackageUpdate() = runBlocking {
    apk()
    val repository = AndroidKotlinVersionRepository(installedApps)
    assertEquals(null, repository.getVersion(info.packageName))
    assertEquals(null, repository.getVersion(info.packageName))
    assertEquals(1, componentReads)
    info.lastUpdateTime++
    repository.getVersion(info.packageName)
    assertEquals(2, componentReads)
  }

  @Test
  fun retriesReadFailuresInsteadOfCachingUnknown() = runBlocking {
    apk(corruptDex = true)
    val repository = AndroidKotlinVersionRepository(installedApps)
    assertEquals(null, repository.getVersion(info.packageName))
    assertEquals(null, repository.getVersion(info.packageName))
    assertEquals(2, componentReads)
  }

  @Test
  fun invalidatesWhenSourcePathChangesWithoutTimestampChange() = runBlocking {
    apk()
    val repository = AndroidKotlinVersionRepository(installedApps)
    repository.getVersion(info.packageName)
    apk()
    repository.getVersion(info.packageName)
    assertEquals(2, componentReads)
  }

  private fun apk(corruptDex: Boolean = false) {
    val file = temporaryFolder.newFile()
    ZipOutputStream(file.outputStream()).use { zip ->
      if (corruptDex) {
        zip.putNextEntry(ZipEntry("classes.dex"))
        zip.write(byteArrayOf(0, 1, 2))
        zip.closeEntry()
      }
    }
    info.applicationInfo = ApplicationInfo().apply { sourceDir = file.path }
  }
}
