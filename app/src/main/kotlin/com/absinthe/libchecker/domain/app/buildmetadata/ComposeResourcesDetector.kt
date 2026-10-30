package com.absinthe.libchecker.domain.app.buildmetadata

import android.content.pm.PackageInfo
import com.absinthe.libchecker.compat.IZipFile
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.PackageUtils.getSplitsSourceDir
import com.absinthe.libchecker.utils.dex.DexArchiveScanner
import com.absinthe.libchecker.utils.dex.DexArchiveScanner.findDexClasses
import com.absinthe.libchecker.utils.extensions.toClassDefType
import com.absinthe.libchecker.utils.manifest.ManifestReader
import java.io.File
import java.util.concurrent.CancellationException

object ComposeResourcesDetector {
  private const val COMPOSE_RESOURCES_ASSET_PREFIX = "assets/composeResources/"

  private const val COMPOSE_RESOURCES_PROVIDER = "org.jetbrains.compose.resources.AndroidContextProvider"

  private val COMPOSE_RESOURCES_CLASS_PATTERN = "org.jetbrains.compose.resources.*".toClassDefType()

  private val COMPOSE_RESOURCES_CLASS_PATTERNS = listOf(COMPOSE_RESOURCES_CLASS_PATTERN)

  fun PackageInfo.usesComposeMultiplatformResources(openedBaseZip: IZipFile? = null): Boolean {
    val sourceDir = applicationInfo?.sourceDir ?: return false
    val openedSplits = mutableListOf<IZipFile>()
    return try {
      val apkFiles = buildList {
        add(File(sourceDir))
        applicationInfo?.splitSourceDirs?.forEach { add(File(it)) }
        PackageUtils.getSplitsSourceDir(this@usesComposeMultiplatformResources)?.forEach { add(File(it)) }
      }.distinct()
      if (apkFiles.any { !it.isFile || !it.canRead() }) return false

      val apkZips = apkFiles.map { apkFile ->
        if (apkFile.path == sourceDir && openedBaseZip != null) {
          openedBaseZip
        } else {
          ZipFileCompat(apkFile).also(openedSplits::add)
        }
      }
      hasMatchingComposeResourcesApkSet(apkZips) && hasComposeMultiplatformResources(apkZips)
    } catch (e: Throwable) {
      if (e is CancellationException) throw e
      false
    } finally {
      openedSplits.forEach(IZipFile::close)
    }
  }

  private fun PackageInfo.hasMatchingComposeResourcesApkSet(apkZips: List<IZipFile>): Boolean {
    val manifests = apkZips.map { zip ->
      val entry = zip.getEntry("AndroidManifest.xml") ?: return false
      val bytes = zip.getInputStream(entry).use(ManifestReader::getBytesFromInputStream) ?: return false
      ManifestReader.getApkIdentity(bytes)
    }
    val base = manifests.first()
    if (packageName.isNullOrBlank() || base["package"] != packageName || base.containsKey("split")) return false
    val splitNames = mutableSetOf<String>()
    return manifests.all { manifest ->
      manifest["package"] == packageName &&
        listOf("versionCode", "versionCodeMajor").all { attribute ->
          java.lang.Long.decode(manifest[attribute] ?: "0") ==
            java.lang.Long.decode(base[attribute] ?: "0")
        }
    } && manifests.drop(1).all { manifest ->
      val splitName = manifest["split"]
      splitName.isNullOrBlank().not() && splitNames.add(splitName)
    }
  }

  internal fun PackageInfo.hasComposeMultiplatformResources(apkZips: List<IZipFile>): Boolean {
    val hasResourcesPayload = apkZips.any { zip ->
      zip.getZipEntries().asSequence().any { entry ->
        !entry.isDirectory && entry.name.startsWith(COMPOSE_RESOURCES_ASSET_PREFIX)
      }
    }
    if (!hasResourcesPayload) return false

    val hasResourcesProvider = providers?.any { it.name == COMPOSE_RESOURCES_PROVIDER } == true
    return hasResourcesProvider || apkZips.any { zip ->
      DexArchiveScanner.findDexClasses(zip, COMPOSE_RESOURCES_CLASS_PATTERNS)
        .contains(COMPOSE_RESOURCES_CLASS_PATTERN)
    }
  }
}
