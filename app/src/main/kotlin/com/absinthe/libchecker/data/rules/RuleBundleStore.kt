package com.absinthe.libchecker.data.rules

import android.util.AtomicFile
import com.absinthe.libchecker.domain.rules.RuleBundleManifest
import com.absinthe.libchecker.domain.rules.RuleBundleMetadata
import com.absinthe.libchecker.utils.JsonUtil
import com.absinthe.rulesbundle.RuleReader
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import okio.HashingSink
import okio.blackholeSink
import okio.buffer
import okio.source
import org.apache.commons.io.IOUtils

/** Immutable directories remain available to readers until process restart. */
class RuleBundleStore(
  private val root: File,
  private val validateDatabase: (File) -> Unit = { RuleReader.open(it).use { } },
  private val readPointer: (File) -> ByteArray = { AtomicFile(it).readFully() },
  private val writePointer: (File, ByteArray) -> Unit = { file, bytes ->
    val atomic = AtomicFile(file)
    val stream = atomic.startWrite()
    try {
      stream.write(bytes)
      atomic.finishWrite(stream)
    } catch (failure: Throwable) {
      atomic.failWrite(stream)
      throw failure
    }
  }
) {
  data class Installed(val directory: File, val manifest: RuleBundleManifest) {
    val database: File get() = File(directory, "rules.db")
  }

  fun installed(manifest: RuleBundleManifest): Installed? {
    val directory = File(root, manifest.androidArtifact().sha256)
    return Installed(directory, manifest).takeIf { it.database.isFile && File(directory, "archive.zip").isFile }
  }

  @Synchronized
  fun candidates(): List<Installed> = listOf("current", "previous").mapNotNull { pointer ->
    runCatching {
      val digest = readPointer(File(root, pointer)).toString(Charsets.UTF_8).trim().also { require(RuleBundleManifest.SHA256.matches(it)) }
      val directory = File(root, digest)
      val manifest = parseManifest(File(directory, "manifest.json").readText())
      require(manifest.androidArtifact().sha256 == digest)
      Installed(directory, manifest)
    }.getOrNull()
  }.distinctBy { it.directory }

  @Synchronized
  fun install(manifest: RuleBundleManifest, archive: File, activate: Boolean = true, repairExisting: Boolean = false): Installed {
    val artifact = manifest.androidArtifact()
    require(archive.length() == artifact.size && archive.sha256() == artifact.sha256) { "Rule archive checksum/size mismatch" }
    if (activate) {
      val latest = candidates().maxByOrNull { it.manifest.dataVersion }
      require(latest == null || manifest.dataVersion > latest.manifest.dataVersion || artifact.sha256 == latest.directory.name) {
        "Rule rollback requires a forward dataVersion"
      }
    }
    check(root.mkdirs() || root.isDirectory)
    val staging = File(root, "staging")
    staging.deleteRecursively()
    check(staging.mkdir())
    try {
      extract(archive, staging)
      val metadata = JsonUtil.moshi.adapter(RuleBundleMetadata::class.java)
        .fromJson(File(staging, "metadata.json").readText())
      require(metadata == manifest.metadata) { "Rule metadata does not match manifest" }
      validateDatabase(File(staging, "rules.db"))
      archive.inputStream().use { input ->
        FileOutputStream(File(staging, "archive.zip")).use { output ->
          input.copyTo(output)
          output.fd.sync()
        }
      }
      File(staging, "manifest.json").writeSynced(JsonUtil.moshi.adapter(RuleBundleManifest::class.java).toJson(manifest).toByteArray())
      val target = File(root, artifact.sha256)
      if (target.exists()) {
        val intact = staging.walkTopDown().filter(File::isFile).all { file ->
          val existing = File(target, file.relativeTo(staging).path)
          existing.isFile && existing.sha256() == file.sha256()
        } && !File(target, "invalid").exists()
        if (!intact) {
          check(repairExisting) { "Installed rule bundle was modified" }
          val quarantine = File(root, "damaged-${artifact.sha256}")
          quarantine.deleteRecursively()
          check(target.renameTo(quarantine)) { "Cannot isolate damaged rules" }
          if (!staging.renameTo(target)) {
            check(quarantine.renameTo(target))
            error("Cannot restore verified rules")
          }
          quarantine.deleteRecursively()
        }
      } else {
        check(staging.renameTo(target)) { "Cannot publish rule bundle" }
      }
      if (activate) {
        candidates().firstOrNull { runCatching { validateInstalled(it) }.isSuccess }?.directory?.name?.takeIf { it != artifact.sha256 }?.let { publish("previous", it) }
        publish("current", artifact.sha256)
      }
      return Installed(target, manifest)
    } finally {
      staging.deleteRecursively()
    }
  }

  fun validateInstalled(installed: Installed) {
    require(!File(installed.directory, "invalid").exists())
    val artifact = installed.manifest.androidArtifact()
    val archive = File(installed.directory, "archive.zip")
    require(archive.length() == artifact.size && archive.sha256() == artifact.sha256)
    ZipFile(archive).use { zip ->
      require(zip.size() == 2 && zip.entries().asSequence().map { it.name }.toSet() == REQUIRED_FILES)
      zip.entries().asSequence().forEach { entry ->
        require(allowedPath(entry.name))
        val file = File(installed.directory, entry.name)
        require(file.isFile && file.length() == entry.size)
        val sink = HashingSink.sha256(blackholeSink())
        zip.getInputStream(entry).source().buffer().use { it.readAll(sink) }
        require(file.sha256() == sink.hash.hex()) { "Installed rule file changed" }
      }
    }
    val metadata = JsonUtil.moshi.adapter(RuleBundleMetadata::class.java).fromJson(File(installed.directory, "metadata.json").readText())
    require(metadata == installed.manifest.metadata)
    validateDatabase(installed.database)
  }

  private fun extract(archive: File, directory: File) {
    ZipFile(archive).use { zip ->
      val entries = zip.entries().asSequence().toList()
      require(entries.size == 2 && entries.map { it.name }.toSet() == REQUIRED_FILES)
      require(entries.map { it.name }.toSet().size == entries.size) { "Duplicate archive entries" }
      var total = 0L
      for (entry in entries) {
        require(!entry.isDirectory && allowedPath(entry.name)) { "Unexpected rule archive path" }
        val maximum = when {
          entry.name == "rules.db" -> 32L * 1024 * 1024
          else -> 64L * 1024
        }
        val destination = File(directory, entry.name)
        check(destination.parentFile!!.mkdirs() || destination.parentFile!!.isDirectory)
        zip.getInputStream(entry).use { input ->
          FileOutputStream(destination).use { output ->
            val copyLimit = minOf(maximum, MAX_EXPANDED_BYTES - total)
            require(copyLimit >= 0) { "Rule archive exceeds extraction limits" }
            val count = IOUtils.copyLarge(input, output, 0, copyLimit)
            require(count < copyLimit || input.read() == -1) { "Rule archive exceeds extraction limits" }
            total += count
            output.fd.sync()
          }
        }
      }
      require(REQUIRED_FILES.all { File(directory, it).isFile })
    }
  }

  /** Call only while the repository holds the reader lock and no readers are using this download store. */
  @Synchronized
  fun pruneUnreferenced(retained: Set<File>, baselineVersion: Int = 0) {
    val keep = (retained + candidates().filter { it.manifest.dataVersion > baselineVersion && runCatching { validateInstalled(it) }.isSuccess }.map { it.directory }).map { it.canonicalFile }.toSet()
    root.listFiles()?.filter { RuleBundleManifest.SHA256.matches(it.name) && it.canonicalFile !in keep }
      ?.forEach { directory -> check(directory.deleteRecursively()) { "Cannot remove unused rule dataset" } }
  }

  private fun publish(name: String, digest: String) {
    writePointer(File(root, name), digest.toByteArray())
  }

  companion object {
    const val MAX_ARCHIVE_BYTES = 32L * 1024 * 1024
    private const val MAX_EXPANDED_BYTES = 64L * 1024 * 1024
    private val REQUIRED_FILES = setOf("rules.db", "metadata.json")

    internal fun allowedPath(path: String): Boolean = path in REQUIRED_FILES

    fun parseManifest(text: String): RuleBundleManifest = requireNotNull(JsonUtil.moshi.adapter(RuleBundleManifest::class.java).fromJson(text)).also { it.androidArtifact() }

    internal fun File.sha256(): String {
      val sink = HashingSink.sha256(blackholeSink())
      source().buffer().use { it.readAll(sink) }
      return sink.hash.hex()
    }

    private fun File.writeSynced(bytes: ByteArray) = FileOutputStream(this).use {
      it.write(bytes)
      it.fd.sync()
    }
  }
}
