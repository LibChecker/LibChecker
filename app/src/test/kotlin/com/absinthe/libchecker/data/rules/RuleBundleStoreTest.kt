package com.absinthe.libchecker.data.rules

import com.absinthe.libchecker.data.rules.RuleBundleStore.Companion.sha256
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.rules.RuleBundleArtifact
import com.absinthe.libchecker.domain.rules.RuleBundleManifest
import com.absinthe.libchecker.domain.rules.RuleBundleMetadata
import com.absinthe.libchecker.utils.JsonUtil
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RuleBundleStoreTest {
  @get:Rule val temporary = TemporaryFolder()

  private val archive: File by lazy {
    temporary.newFile("android-v5.zip").also { archive ->
      ZipFile(File(requireNotNull(System.getProperty("rulesBundleAar")))).use { aar ->
        ZipOutputStream(archive.outputStream()).use { output ->
          listOf("rules.db", "metadata.json").forEach { name ->
            output.putNextEntry(ZipEntry(name))
            aar.getInputStream(aar.getEntry("assets/lcrules/v5/$name")).use { it.copyTo(output) }
            output.closeEntry()
          }
        }
      }
    }
  }

  private fun manifest(): RuleBundleManifest {
    val metadata = ZipFile(archive).use { zip ->
      requireNotNull(JsonUtil.moshi.adapter(RuleBundleMetadata::class.java).fromJson(zip.getInputStream(zip.getEntry("metadata.json")).bufferedReader().use { it.readText() }))
    }
    return RuleBundleManifest(
      metadata.schemaVersion,
      metadata.dataVersion,
      metadata.sourceRevision,
      metadata.compilerRevision,
      metadata.contentSha256,
      metadata.ruleCount,
      metadata.minimumReader,
      mapOf("android" to RuleBundleArtifact("releases/${metadata.dataVersion}/android-v5.zip", archive.sha256(), archive.length(), 5, 5))
    )
  }
  private fun store() = RuleBundleStore(
    temporary.root,
    validateDatabase = { require(it.length() > 0) },
    readPointer = File::readBytes,
    writePointer = { file, bytes -> file.writeBytes(bytes) }
  )

  @Test fun installsArchiveFromPublishedAarDataAndDetectsTampering() {
    val store = store()
    val expected = manifest()
    val installed = store.install(expected, archive)
    assertEquals(expected.ruleCount, installed.manifest.ruleCount)
    assertEquals(expected.dataVersion, store.candidates().single().manifest.dataVersion)
    store.validateInstalled(installed)
    File(installed.directory, "rules.db").appendText("corrupt")
    assertThrows(IllegalArgumentException::class.java) { store.validateInstalled(installed) }
    val repaired = store.install(expected, archive, repairExisting = true)
    store.validateInstalled(repaired)
    assertEquals(expected.dataVersion, store.candidates().single().manifest.dataVersion)
  }

  @Test fun removesOnlyRetiredDatabaseFilesAndVersionMarkers() {
    val noBackup = temporary.newFolder("no_backup")
    val files = temporary.newFolder("files")
    val db = temporary.newFile("lcrules_database")
    val old = File(noBackup, "lcrules/rules-v44.db").apply {
      parentFile!!.mkdirs()
      writeText("old")
    }
    val marker = File(files, "lcrules/version/999999").apply {
      parentFile!!.mkdirs()
      writeText("")
    }
    val unrelated = File(noBackup, "lcrules/keep.txt").apply { writeText("keep") }
    val current = File(noBackup, "rules-v5/current").apply {
      parentFile!!.mkdirs()
      writeText("keep")
    }
    RulesRepository.pruneLegacyRules(noBackup, files, db)
    assertTrue(!old.exists() && !marker.exists() && !db.exists())
    assertTrue(unrelated.exists() && current.exists())
  }

  @Test fun retainsPreviousAndRejectsBackwardVersions() {
    val store = store()
    val original = manifest()
    store.install(original, archive)
    val (next, nextArchive) = nextRelease(original)
    store.install(next, nextArchive)
    assertEquals(listOf(next.dataVersion, original.dataVersion), store.candidates().map { it.manifest.dataVersion })
    assertThrows(IllegalArgumentException::class.java) { store.install(original, archive) }
    val current = store.candidates().first()
    File(current.directory, "rules.db").appendText("corrupt")
    assertThrows(IllegalArgumentException::class.java) { store.validateInstalled(current) }
    store.validateInstalled(store.candidates().last())
  }

  @Test fun pointerWriteFailureKeepsTheOldSelection() {
    val original = manifest()
    store().install(original, archive)
    val (next, nextArchive) = nextRelease(original)
    val failing = RuleBundleStore(
      temporary.root,
      validateDatabase = { },
      readPointer = File::readBytes,
      writePointer = { file, bytes -> if (file.name == "current") error("injected pointer failure") else file.writeBytes(bytes) }
    )
    assertThrows(IllegalStateException::class.java) { failing.install(next, nextArchive) }
    assertEquals(original.dataVersion, store().candidates().first().manifest.dataVersion)
  }

  @Test fun prunesObsoleteFullPreviewButKeepsValidCurrentPreviousAndBundled() {
    val store = store()
    val original = manifest()
    val bundled = store.install(original, archive, activate = false)
    val (next, nextArchive) = nextRelease(original)
    val current = store.install(next, nextArchive)
    val obsolete = File(temporary.root, "a".repeat(64)).apply { mkdir() }
    File(obsolete, "details").mkdir()
    File(obsolete, "manifest.json").writeText(
      JsonUtil.moshi.adapter(RuleBundleManifest::class.java).toJson(
        original.copy(artifacts = original.artifacts + ("android" to original.androidArtifact().copy(sha256 = obsolete.name)))
      )
    )
    File(temporary.root, "previous").writeText(obsolete.name)
    store.pruneUnreferenced(setOf(bundled.directory))
    assertTrue(!obsolete.exists())
    assertTrue(current.database.exists())
    assertTrue(bundled.database.exists())
    File(temporary.root, "previous").writeText(bundled.directory.name)
    store.pruneUnreferenced(emptySet())
    assertTrue(bundled.database.exists())
    store.pruneUnreferenced(emptySet(), original.dataVersion)
    assertTrue(!bundled.directory.exists())
    assertTrue(current.database.exists())
  }

  @Test fun rejectsFullContentArchiveEvenWithValidChecksum() {
    val fullArchive = temporary.newFile("full.zip")
    ZipOutputStream(fullArchive.outputStream()).use { output ->
      ZipFile(archive).use { input ->
        input.entries().asSequence().forEach { entry ->
          output.putNextEntry(ZipEntry(entry.name))
          input.getInputStream(entry).use { it.copyTo(output) }
          output.closeEntry()
        }
      }
      output.putNextEntry(ZipEntry("details/preview.json"))
      output.write("{}".toByteArray())
      output.closeEntry()
    }
    val original = manifest()
    val full = original.copy(artifacts = original.artifacts + ("android" to original.androidArtifact().copy(sha256 = fullArchive.sha256(), size = fullArchive.length())))
    assertThrows(IllegalArgumentException::class.java) { store().install(full, fullArchive) }
  }

  private fun nextRelease(original: RuleBundleManifest): Pair<RuleBundleManifest, File> {
    val next = original.copy(dataVersion = original.dataVersion + 1)
    val nextArchive = temporary.newFile("next.zip")
    ZipOutputStream(nextArchive.outputStream()).use { output ->
      ZipFile(archive).use { input ->
        input.entries().asSequence().forEach { entry ->
          output.putNextEntry(ZipEntry(entry.name))
          if (entry.name == "metadata.json") {
            output.write(JsonUtil.moshi.adapter(com.absinthe.libchecker.domain.rules.RuleBundleMetadata::class.java).toJson(next.metadata).toByteArray())
          } else {
            input.getInputStream(entry).use { it.copyTo(output) }
          }
          output.closeEntry()
        }
      }
    }
    return next.copy(
      artifacts = next.artifacts + (
        "android" to next.artifacts.getValue("android").copy(
          path = "releases/${next.dataVersion}/android-v5.zip",
          sha256 = nextArchive.sha256(),
          size = nextArchive.length()
        )
        )
    ) to nextArchive
  }

  @Test fun badChecksumDoesNotReplaceCurrentAndTraversalIsRejected() {
    val store = store()
    val expected = manifest()
    store.install(expected, archive)
    val bad = temporary.newFile("bad.zip")
    ZipOutputStream(bad.outputStream()).use { zip ->
      zip.putNextEntry(ZipEntry("../outside"))
      zip.write(byteArrayOf(1))
      zip.closeEntry()
    }
    assertThrows(IllegalArgumentException::class.java) { store.install(expected, bad) }
    assertEquals(expected.dataVersion, store.candidates().single().manifest.dataVersion)
    listOf("../outside", "/rules.db", "icons/../../outside.svg", "details/x.json", "rules.db-wal").forEach {
      assertTrue(!RuleBundleStore.allowedPath(it))
    }
  }
}
