package com.absinthe.libchecker.utils.apk

import com.absinthe.libchecker.compat.ZipFileCompat
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.Random
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.math.roundToLong
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Repeatable host-side benchmarks for ZIP paths changed by the ZIP optimization work. */
class ZipOptimizationBenchmarkTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun benchmarkZipScenarios() {
    val results = mutableListOf<Result>()
    results += benchmarkEntryLookup()
    results += benchmarkFeatureStyleScan()
    results += benchmarkArchiveStaging()
    results += benchmarkOffsetBatching()
    results += benchmarkArchiveWriting()
    results += benchmarkDigest()
    results += benchmarkEocdSearch()

    val report = buildString {
      appendLine("scenario,before_ms,after_ms,change_percent")
      results.forEach { result ->
        appendLine(
          "${result.name},${result.beforeMs.format()},${result.afterMs.format()},${result.changePercent.format()}"
        )
      }
    }
    val reportFile = File("build/reports/zip-optimization-benchmark.csv")
    reportFile.parentFile?.mkdirs()
    reportFile.writeText(report)
    println("ZIP_BENCHMARK_REPORT=${reportFile.absolutePath}")
    println(report)
  }

  private fun benchmarkEntryLookup(): Result {
    val archive = createZip("lookup.zip", 5_000, 128)
    val target = "assets/entry-4750.bin"
    return compare(
      "exact_entry_lookup",
      iterations = 15,
      before = {
        ZipFileCompat(archive).use { zip ->
          check(zip.getZipEntries().asSequence().firstOrNull { it.name == target } != null)
        }
      },
      after = {
        ZipFileCompat(archive).use { zip -> check(zip.getEntry(target) != null) }
      }
    )
  }

  private fun benchmarkFeatureStyleScan(): Result {
    val archive = createZip("features.apk", 5_000, 128, featureEntries = true)
    return compare(
      "feature_style_zip_scan",
      iterations = 12,
      before = {
        repeat(3) { index ->
          ZipFileCompat(archive).use { zip -> check(zip.getEntry(FEATURE_ENTRIES[index]) != null) }
        }
        ZipFileCompat(archive).use { zip ->
          check(zip.getZipEntries().asSequence().any { it.name.endsWith(".version") })
        }
      },
      after = {
        ZipFileCompat(archive).use { zip ->
          repeat(3) { index -> check(zip.getEntry(FEATURE_ENTRIES[index]) != null) }
          check(zip.getZipEntries().asSequence().any { it.name.endsWith(".version") })
        }
      }
    )
  }

  private fun benchmarkArchiveStaging(): List<Result> {
    val archive = createZip("bundle.apks", 32, 256 * 1024, apkEntries = true)
    val legacyRoot = temporaryFolder.newFolder("legacy-staging")
    val before = measure(iterations = 5) { iteration ->
      legacyStage(archive, File(legacyRoot, iteration.toString()))
    }
    val coldOptimized = measure(iterations = 5) { iteration ->
      val coldRoot = temporaryFolder.newFolder("optimized-cold-$iteration")
      ZipFileCompat(archive).use { zip ->
        ApkArchiveStager.stage(archive, coldRoot, zip, apkEntries(zip))
      }
    }
    val optimizedRoot = temporaryFolder.newFolder("optimized-cached")
    ZipFileCompat(archive).use { zip ->
      ApkArchiveStager.stage(archive, optimizedRoot, zip, apkEntries(zip))
    }
    val cachedOptimized = median(
      (0 until 5).map {
        elapsedMs {
          ZipFileCompat(archive).use { zip ->
            ApkArchiveStager.stage(archive, optimizedRoot, zip, apkEntries(zip))
          }
        }
      }
    )
    return listOf(
      Result("cold_archive_staging", before, coldOptimized),
      Result("cached_archive_staging", before, cachedOptimized)
    )
  }

  private fun benchmarkOffsetBatching(): Result {
    val archive = createZip("offsets.apk", 500, 256)
    val names = (400 until 450).map { "assets/entry-$it.bin" }.toSet()
    return compare(
      "central_directory_offset_batch",
      iterations = 8,
      before = { names.forEach { ZipDataOffsetReader.readFromCentralDirectory(archive, setOf(it)) } },
      after = { check(ZipDataOffsetReader.readFromCentralDirectory(archive, names).size == names.size) }
    )
  }

  private fun benchmarkArchiveWriting(): Result {
    val inputs = List(4) { index ->
      temporaryFolder.newFile("input-$index.apk").apply { writeBytes(randomBytes(2 * 1024 * 1024, index.toLong())) }
    }
    return compare(
      "apks_outer_archive_write",
      iterations = 5,
      before = { writeOuterArchive(temporaryFolder.newFile(), inputs, Deflater.DEFAULT_COMPRESSION) },
      after = { writeOuterArchive(temporaryFolder.newFile(), inputs, Deflater.NO_COMPRESSION) }
    )
  }

  private fun benchmarkDigest(): Result {
    val input = temporaryFolder.newFile("digest.bin").apply {
      writeBytes(randomBytes(16 * 1024 * 1024, 42))
    }
    return compare(
      "sha256_memory_strategy",
      iterations = 8,
      before = { MessageDigest.getInstance("SHA-256").digest(input.readBytes()) },
      after = {
        val digest = MessageDigest.getInstance("SHA-256")
        BufferedInputStream(input.inputStream()).use { stream ->
          val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
          while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
          }
        }
        digest.digest()
      }
    )
  }

  private fun benchmarkEocdSearch(): Result {
    val bytes = randomBytes(65_557, 7).apply {
      val offset = size - 22
      EOCD_SIGNATURE.copyInto(this, offset)
      this[offset + 20] = 0
      this[offset + 21] = 0
    }
    return compare(
      "eocd_search",
      iterations = 1_000,
      before = { check(bytes.indexOfSequence(EOCD_SIGNATURE) >= 0) },
      after = { check(findTerminalEocd(bytes) >= 0) }
    )
  }

  private fun apkEntries(zip: ZipFileCompat): List<Pair<ZipEntry, String>> = zip.getZipEntries().asSequence().map { it to it.name }.toList()

  private fun legacyStage(archive: File, output: File) {
    output.mkdirs()
    ZipFile(archive).use { zip ->
      zip.entries().asSequence().forEach { entry ->
        zip.getInputStream(entry).use { input ->
          File(output, entry.name).outputStream().use { input.copyTo(it) }
        }
      }
    }
  }

  private fun writeOuterArchive(output: File, inputs: List<File>, level: Int) {
    ZipOutputStream(BufferedOutputStream(output.outputStream())).use { zip ->
      zip.setLevel(level)
      inputs.forEach { input ->
        zip.putNextEntry(ZipEntry(input.name))
        input.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
      }
    }
  }

  private fun createZip(
    name: String,
    count: Int,
    payloadSize: Int,
    featureEntries: Boolean = false,
    apkEntries: Boolean = false
  ): File {
    val archive = temporaryFolder.newFile(name)
    val payload = randomBytes(payloadSize, 1)
    ZipOutputStream(BufferedOutputStream(archive.outputStream())).use { zip ->
      repeat(count) { index ->
        val entryName = if (apkEntries) {
          if (index == 0) "base.apk" else "split_config.$index.apk"
        } else {
          "assets/entry-$index.bin"
        }
        zip.putNextEntry(ZipEntry(entryName))
        zip.write(payload)
        zip.closeEntry()
      }
      if (featureEntries) {
        FEATURE_ENTRIES.forEach { entryName ->
          zip.putNextEntry(ZipEntry(entryName))
          zip.write("1.0".toByteArray())
          zip.closeEntry()
        }
      }
    }
    return archive
  }

  private fun compare(
    name: String,
    iterations: Int,
    before: () -> Unit,
    after: () -> Unit
  ): Result {
    repeat(2) {
      before()
      after()
    }
    return Result(name, measure(iterations) { before() }, measure(iterations) { after() })
  }

  private fun measure(iterations: Int, block: (Int) -> Unit): Double = median((0 until iterations).map { iteration -> elapsedMs { block(iteration) } })

  private fun elapsedMs(block: () -> Unit): Double {
    val start = System.nanoTime()
    block()
    return (System.nanoTime() - start) / 1_000_000.0
  }

  private fun median(values: List<Double>): Double = values.sorted()[values.size / 2]

  private fun findTerminalEocd(bytes: ByteArray): Int {
    for (index in bytes.size - 22 downTo 0) {
      if (matchesAt(bytes, index, EOCD_SIGNATURE)) return index
    }
    return -1
  }

  private fun randomBytes(size: Int, seed: Long): ByteArray = ByteArray(size).also {
    Random(seed).nextBytes(it)
  }

  private fun matchesAt(bytes: ByteArray, offset: Int, expected: ByteArray): Boolean {
    if (offset < 0 || offset + expected.size > bytes.size) return false
    return expected.indices.all { bytes[offset + it] == expected[it] }
  }

  private fun Double.format(): String = ((this * 1_000).roundToLong() / 1_000.0).toString()

  private data class Result(val name: String, val beforeMs: Double, val afterMs: Double) {
    val changePercent: Double
      get() = (afterMs - beforeMs) / beforeMs * 100
  }

  private companion object {
    val FEATURE_ENTRIES = arrayOf(
      "kotlin-tooling-metadata.json",
      "META-INF/com/android/build/gradle/app-metadata.properties",
      "META-INF/xposed/module.prop",
      "META-INF/androidx.compose.ui_ui.version"
    )
    val EOCD_SIGNATURE = byteArrayOf(0x50, 0x4B, 0x05, 0x06)
  }
}
