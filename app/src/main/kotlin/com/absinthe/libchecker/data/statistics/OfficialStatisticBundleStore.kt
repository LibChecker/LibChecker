package com.absinthe.libchecker.data.statistics

import com.absinthe.libchecker.domain.statistics.chart.model.STATISTIC_SCHEMA_VERSION
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticBundle
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticRemoteManifest
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticSource
import com.absinthe.libchecker.domain.statistics.chart.usecase.ValidateStatisticCatalogUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.ValidateStatisticSvgUseCase
import com.absinthe.libchecker.utils.JsonUtil
import com.absinthe.libchecker.utils.extensions.sha256
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class OfficialStatisticBundleStore(
  private val rootDirectory: File,
  private val validateCatalog: ValidateStatisticCatalogUseCase,
  private val validateSvg: ValidateStatisticSvgUseCase
) {

  val downloadFile: File
    get() = File(rootDirectory, DOWNLOAD_FILE)

  val currentSha256: String?
    get() = (currentPointer.takeIf { it.isFile } ?: backupPointer.takeIf { it.isFile })
      ?.readText()
      ?.trim()
      ?.takeIf(SHA_256::matches)

  fun loadCachedStatistics(): List<StatisticDefinition> {
    val sha256 = currentSha256 ?: return emptyList()
    return runCatching { loadInstalledBundle(File(bundlesDirectory, sha256)) }
      .getOrDefault(emptyList())
  }

  fun install(
    manifest: StatisticRemoteManifest,
    bundleFile: File
  ): List<StatisticDefinition> {
    validateManifest(manifest)
    check(bundleFile.length() == manifest.bundleSize) {
      "Chart bundle size does not match its manifest"
    }
    check(bundleFile.readBytes().sha256().lowercase() == manifest.bundleSha256) {
      "Chart bundle digest does not match its manifest"
    }

    val parsedBundle = parseBundle(bundleFile)
    val targetDirectory = File(bundlesDirectory, manifest.bundleSha256)
    if (!targetDirectory.isDirectory) {
      val stagingDirectory = File(rootDirectory, "staging-${manifest.bundleSha256}")
      stagingDirectory.deleteRecursively()
      check(stagingDirectory.mkdirs()) { "Unable to create chart bundle staging directory" }
      File(stagingDirectory, CATALOG_FILE).writeBytes(parsedBundle.catalog)
      parsedBundle.icons.forEach { (path, bytes) ->
        val iconFile = File(stagingDirectory, path)
        check(iconFile.parentFile?.ensureDirectoryExists() == true) {
          "Unable to create chart icon directory"
        }
        iconFile.writeBytes(bytes)
      }
      check(bundlesDirectory.mkdirs() || bundlesDirectory.isDirectory) {
        "Unable to create chart bundle directory"
      }
      check(stagingDirectory.renameTo(targetDirectory)) {
        "Unable to publish chart bundle"
      }
    }
    publishCurrent(manifest.bundleSha256)
    removeOldBundles(manifest.bundleSha256)
    return loadInstalledBundle(targetDirectory)
  }

  private fun parseBundle(bundleFile: File): ParsedBundle {
    ZipFile(bundleFile).use { archive ->
      val allEntries = archive.entries().asSequence().toList()
      check(allEntries.size <= MAX_ZIP_ENTRIES) { "Chart bundle contains too many entries" }
      check(allEntries.none(ZipEntry::isDirectory)) {
        "Chart bundle contains unexpected directory entries"
      }
      val entries = allEntries.filterNot(ZipEntry::isDirectory)
      val names = entries.map(ZipEntry::getName)
      check(names.size == names.toSet().size) { "Chart bundle contains duplicate entries" }

      val catalogEntry = checkNotNull(archive.getEntry(CATALOG_FILE)) {
        "Chart bundle catalog is missing"
      }
      val catalogBytes = archive.getInputStream(catalogEntry).use {
        it.readLimited(MAX_CATALOG_BYTES)
      }
      val bundle = parseAndValidateCatalog(catalogBytes)
      val referencedIcons = bundle.definitions.map { definition ->
        checkNotNull(definition.icon.asset)
      }.toSet()
      check(names.toSet() == referencedIcons + CATALOG_FILE) {
        "Chart bundle contains missing or unreferenced files"
      }

      var totalBytes = catalogBytes.size.toLong()
      val icons = referencedIcons.associateWith { path ->
        val entry = checkNotNull(archive.getEntry(path)) { "Chart icon is missing: $path" }
        val bytes = archive.getInputStream(entry).use {
          it.readLimited(ValidateStatisticSvgUseCase.MAX_SVG_BYTES)
        }
        val errors = validateSvg(bytes)
        check(errors.isEmpty()) { "Invalid chart SVG $path: ${errors.joinToString()}" }
        totalBytes += bytes.size
        check(totalBytes <= MAX_UNCOMPRESSED_BYTES) {
          "Chart bundle expands beyond the allowed size"
        }
        bytes
      }
      return ParsedBundle(catalogBytes, icons)
    }
  }

  private fun loadInstalledBundle(directory: File): List<StatisticDefinition> {
    check(directory.isDirectory) { "Chart bundle directory is missing" }
    val catalogFile = File(directory, CATALOG_FILE)
    check(catalogFile.length() in 1L..MAX_CATALOG_BYTES.toLong()) {
      "Cached chart catalog is invalid"
    }
    val bundle = parseAndValidateCatalog(catalogFile.readBytes())
    return bundle.definitions.map { definition ->
      val asset = checkNotNull(definition.icon.asset)
      val iconFile = File(directory, asset)
      check(iconFile.isFile) { "Cached chart icon is missing: $asset" }
      val iconBytes = iconFile.readBytes()
      check(validateSvg(iconBytes).isEmpty()) { "Cached chart icon is invalid: $asset" }
      definition.copy(icon = definition.icon.copy(localPath = iconFile.absolutePath))
    }
  }

  private fun parseAndValidateCatalog(bytes: ByteArray): StatisticBundle {
    check(bytes.size <= MAX_CATALOG_BYTES) { "Chart catalog is too large" }
    val bundle = checkNotNull(
      JsonUtil.moshi.adapter(StatisticBundle::class.java)
        .fromJson(bytes.toString(Charsets.UTF_8))
    ) { "Chart catalog is empty" }
    check(bundle.definitions.size <= MAX_STATISTICS) { "Chart catalog has too many rules" }
    check(bundle.definitions.all { it.source == StatisticSource.OFFICIAL }) {
      "Chart bundle contains a non-official rule"
    }
    val errors = validateCatalog(bundle)
    check(errors.isEmpty()) { "Invalid chart catalog: ${errors.joinToString()}" }
    return bundle
  }

  fun validateManifest(manifest: StatisticRemoteManifest) {
    check(manifest.schemaVersion == STATISTIC_SCHEMA_VERSION) {
      "Unsupported chart manifest schema: ${manifest.schemaVersion}"
    }
    check(manifest.bundleVersion >= 1) { "Invalid chart bundle version" }
    check(SHA_256.matches(manifest.bundleSha256)) { "Invalid chart bundle digest" }
    check(manifest.bundleSize in 1..MAX_BUNDLE_BYTES) { "Invalid chart bundle size" }
    check(manifest.minimumAppVersionCode >= 0) { "Invalid minimum app version" }
  }

  private fun publishCurrent(sha256: String) {
    check(rootDirectory.mkdirs() || rootDirectory.isDirectory) {
      "Unable to create chart cache directory"
    }
    val temporaryPointer = File(rootDirectory, "$CURRENT_FILE.tmp")
    temporaryPointer.writeText("$sha256\n")
    backupPointer.delete()
    if (currentPointer.isFile) {
      check(currentPointer.renameTo(backupPointer)) {
        "Unable to back up chart bundle pointer"
      }
    }
    if (!temporaryPointer.renameTo(currentPointer)) {
      backupPointer.renameTo(currentPointer)
      error("Unable to publish chart bundle pointer")
    }
    backupPointer.delete()
  }

  private fun removeOldBundles(currentSha256: String) {
    bundlesDirectory.listFiles()?.forEach { directory ->
      if (directory.name != currentSha256) {
        directory.deleteRecursively()
      }
    }
  }

  private fun InputStream.readLimited(maximumBytes: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
      val count = read(buffer)
      if (count < 0) break
      total += count
      check(total <= maximumBytes) { "Chart bundle entry is too large" }
      output.write(buffer, 0, count)
    }
    return output.toByteArray()
  }

  private val bundlesDirectory: File
    get() = File(rootDirectory, BUNDLES_DIRECTORY)

  private val currentPointer: File
    get() = File(rootDirectory, CURRENT_FILE)

  private val backupPointer: File
    get() = File(rootDirectory, "$CURRENT_FILE.bak")

  private data class ParsedBundle(
    val catalog: ByteArray,
    val icons: Map<String, ByteArray>
  )

  companion object {
    const val MAX_BUNDLE_BYTES = 2L * 1024 * 1024
    private const val MAX_CATALOG_BYTES = 256 * 1024
    private const val MAX_UNCOMPRESSED_BYTES = 4L * 1024 * 1024
    private const val MAX_ZIP_ENTRIES = 65
    private const val MAX_STATISTICS = 64
    private const val CATALOG_FILE = "catalog.json"
    private const val BUNDLES_DIRECTORY = "bundles"
    private const val CURRENT_FILE = "current"
    private const val DOWNLOAD_FILE = "chart.bundle.part"
    private val SHA_256 = Regex("[a-f0-9]{64}")
  }
}
