package com.absinthe.libchecker.domain.app.detail.action

import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.elf.ElfParser
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetElfDetailUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(packageName: String, elfPath: String): AppElfDetail? = withContext(Dispatchers.IO) {
    getElfDetail(packageName, elfPath)
  }

  private fun getElfDetail(packageName: String, elfPath: String): AppElfDetail? {
    val packageInfo = installedAppRepository.getPackageInfo(packageName) ?: return null
    val nativePath = packageInfo.applicationInfo?.nativeLibraryDir
    if (nativePath != null) {
      File(nativePath).listFiles()
        ?.find { it.path.endsWith(elfPath) }
        ?.let { return it.readElfDetail() }
    }

    val sourceDir = packageInfo.applicationInfo?.sourceDir ?: return null
    findElfDetailInApk(File(sourceDir), elfPath)?.let { return it }

    PackageUtils.getSplitsSourceDir(packageInfo)?.forEach { split ->
      findElfDetailInApk(File(split), elfPath)?.let { return it }
    }
    return null
  }

  private fun File.readElfDetail(): AppElfDetail {
    val parser = ElfParser(this)
    return parser.readElfDetail()
  }

  private fun findElfDetailInApk(apk: File, elfPath: String): AppElfDetail? {
    ZipFileCompat(apk).use { zipFile ->
      val entry = zipFile.getEntry(elfPath) ?: return null
      val parser = ElfParser(zipFile.getInputStream(entry))
      return parser.readElfDetail()
    }
  }

  private fun ElfParser.readElfDetail(): AppElfDetail {
    return use {
      AppElfDetail(
        deps = parseNeededDependencies(),
        entryPoints = parseEntryPoints(),
        isStripped = isSymbolTableStripped()
      )
    }
  }
}

data class AppElfDetail(
  val deps: List<String>,
  val entryPoints: List<String>,
  val isStripped: Boolean
)
