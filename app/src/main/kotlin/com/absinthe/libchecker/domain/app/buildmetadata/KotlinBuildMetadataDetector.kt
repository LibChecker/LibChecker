package com.absinthe.libchecker.domain.app.buildmetadata

import android.content.pm.PackageInfo
import com.absinthe.libchecker.compat.IZipFile
import com.absinthe.libchecker.domain.app.detail.model.KotlinToolingMetadata
import com.absinthe.libchecker.utils.dex.FastDexFileFactory
import com.absinthe.libchecker.utils.fromJson
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.value.ArrayEncodedValue
import com.android.tools.smali.dexlib2.iface.value.IntEncodedValue
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.InputStreamReader

internal data class KotlinBuildMetadata(
  val kotlinVersion: String? = null,
  val kotlinVersionSource: KotlinVersionSource? = null,
  val gradleVersion: String? = null,
  val javaVersion: String? = null
)

internal data class KotlinVersionInferenceHints(
  val packageName: String,
  val entryPointClassNames: Set<String>
) {
  companion object {
    fun from(
      packageInfo: PackageInfo,
      componentPackageInfo: PackageInfo? = null
    ): KotlinVersionInferenceHints {
      val packageName = packageInfo.packageName.orEmpty()
      val components = componentPackageInfo ?: packageInfo
      val entryPointClassNames = buildSet<String> {
        packageInfo.applicationInfo?.className?.let { add(it.qualifyClassName(packageName)) }
        components.applicationInfo?.className?.let { add(it.qualifyClassName(packageName)) }
        components.activities.orEmpty().forEach { activity ->
          activity.name?.let { add(it.qualifyClassName(packageName)) }
          activity.targetActivity?.let { add(it.qualifyClassName(packageName)) }
        }
        components.services.orEmpty().forEach { service ->
          service.name?.let { add(it.qualifyClassName(packageName)) }
        }
        components.receivers.orEmpty().forEach { receiver ->
          receiver.name?.let { add(it.qualifyClassName(packageName)) }
        }
        components.providers.orEmpty().forEach { provider ->
          provider.name?.let { add(it.qualifyClassName(packageName)) }
        }
        components.instrumentation.orEmpty().forEach { instrumentation ->
          instrumentation.name?.let { add(it.qualifyClassName(packageName)) }
        }
      }.filterTo(linkedSetOf(), String::isNotBlank)
      return KotlinVersionInferenceHints(packageName, entryPointClassNames)
    }
  }
}

private fun String.qualifyClassName(packageName: String): String {
  return when {
    startsWith('.') -> "$packageName$this"
    '.' !in this && packageName.isNotBlank() -> "$packageName.$this"
    else -> this
  }
}

internal enum class KotlinVersionSource {
  TOOLING_METADATA,
  DEX_ENTRY_POINTS,
  DEX_APP_NAMESPACE,
  DEX_METADATA,
  KOTLIN_MODULE
}

internal object KotlinBuildMetadataDetector {

  fun detect(
    apk: File,
    zip: IZipFile,
    inferVersion: Boolean = true,
    inferenceHints: KotlinVersionInferenceHints? = null
  ): KotlinBuildMetadata {
    val toolingMetadata = readToolingMetadata(zip)
    if (toolingMetadata.kotlinVersion != null) {
      return toolingMetadata
    }
    if (!inferVersion) {
      return toolingMetadata
    }

    val dexMetadata = readDexMetadataVersions(apk)
    val scopedDexVersions = dexMetadata.selectVersions(inferenceHints)
    if (scopedDexVersions != null) {
      val version = scopedDexVersions.versions.singleOrNull() ?: return toolingMetadata
      return toolingMetadata.copy(
        kotlinVersion = listOf(version).toDisplayString(),
        kotlinVersionSource = scopedDexVersions.source
      )
    }

    val moduleVersions = readKotlinModuleVersions(zip)
    val moduleVersion = moduleVersions.singleOrNull()
    if (moduleVersion != null) {
      return toolingMetadata.copy(
        kotlinVersion = listOf(moduleVersion).toDisplayString(),
        kotlinVersionSource = KotlinVersionSource.KOTLIN_MODULE
      )
    }
    return toolingMetadata
  }

  fun hasKotlinModuleMetadata(zip: IZipFile): Boolean {
    return zip.getZipEntries().asSequence().any { entry ->
      entry.isDirectory.not() && entry.name.isKotlinModuleMetadataEntry()
    }
  }

  private fun readToolingMetadata(zip: IZipFile): KotlinBuildMetadata {
    val entry = zip.getEntry(KOTLIN_TOOLING_METADATA_ENTRY) ?: return KotlinBuildMetadata()
    return runCatching {
      val json = InputStreamReader(zip.getInputStream(entry), Charsets.UTF_8).use { it.readText() }
      val metadata = json.fromJson<KotlinToolingMetadata>() ?: return@runCatching KotlinBuildMetadata()
      val kotlinAndroidTarget = metadata.projectTargets?.find { target ->
        target.target == KOTLIN_ANDROID_TARGET
      }
      val kotlinVersion = metadata.buildPluginVersion.takeIf { version ->
        version.isNotBlank() &&
          (metadata.buildPlugin == KOTLIN_ANDROID_PLUGIN || kotlinAndroidTarget != null)
      }
      val gradleVersion = metadata.buildSystemVersion.takeIf { version ->
        metadata.buildSystem == GRADLE_BUILD_SYSTEM && version.isNotBlank()
      }
      val sourceCompatibility = kotlinAndroidTarget?.extras?.android?.sourceCompatibility
      val javaVersion = sourceCompatibility?.takeIf { version ->
        version.isNotBlank() && version.all(Char::isDigit)
      }
      KotlinBuildMetadata(
        kotlinVersion = kotlinVersion,
        kotlinVersionSource = kotlinVersion?.let { KotlinVersionSource.TOOLING_METADATA },
        gradleVersion = gradleVersion,
        javaVersion = javaVersion
      )
    }.getOrDefault(KotlinBuildMetadata())
  }

  private fun readDexMetadataVersions(apk: File): List<ClassMetadataVersion> {
    return runCatching {
      val container = FastDexFileFactory.loadDexContainer(apk, Opcodes.getDefault())
      buildList {
        container.dexEntryNames.forEach entryLoop@{ entryName ->
          val dexFile = container.getEntry(entryName)?.dexFile ?: return@entryLoop
          dexFile.classes.forEach classLoop@{ classDef ->
            val metadata = classDef.annotations.firstOrNull { annotation ->
              annotation.type == KOTLIN_METADATA_ANNOTATION
            } ?: return@classLoop
            val metadataVersion = metadata.elements
              .firstOrNull { element -> element.name == KOTLIN_METADATA_VERSION_ELEMENT }
              ?.value as? ArrayEncodedValue
            metadataVersion?.value
              ?.mapNotNull { value -> (value as? IntEncodedValue)?.value }
              ?.toMetadataVersion()
              ?.let { version ->
                add(ClassMetadataVersion(classDef.type.toClassName(), version))
              }
          }
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun List<ClassMetadataVersion>.selectVersions(
    hints: KotlinVersionInferenceHints?
  ): ScopedMetadataVersions? {
    if (isEmpty()) return null
    if (hints != null) {
      val entryPointMetadata = filter { metadata ->
        metadata.className in hints.entryPointClassNames
      }
      val appEntryPointMetadata = entryPointMetadata.filter { metadata ->
        hints.packageName.isNotBlank() && metadata.className.isInNamespace(hints.packageName)
      }
      val entryPointVersions = appEntryPointMetadata.ifEmpty { entryPointMetadata }
        .asSequence()
        .map(ClassMetadataVersion::version)
        .toSet()
      if (entryPointVersions.isNotEmpty()) {
        return ScopedMetadataVersions(entryPointVersions, KotlinVersionSource.DEX_ENTRY_POINTS)
      }

      val appNamespaces = buildSet {
        hints.packageName.takeIf { it.isUsefulNamespace() }?.let(::add)
      }
      val appMetadata = filter { metadata ->
        appNamespaces.any { namespace -> metadata.className.isInNamespace(namespace) }
      }
      if (appMetadata.isNotEmpty()) {
        return ScopedMetadataVersions(
          versions = appMetadata.selectDominantVersions(),
          source = KotlinVersionSource.DEX_APP_NAMESPACE
        )
      }
    }
    return ScopedMetadataVersions(
      versions = mapTo(linkedSetOf(), ClassMetadataVersion::version),
      source = KotlinVersionSource.DEX_METADATA
    )
  }

  private fun List<ClassMetadataVersion>.selectDominantVersions(): Set<MetadataVersion> {
    val counts = groupingBy(ClassMetadataVersion::version).eachCount()
    val dominant = counts.entries.maxWithOrNull(
      compareBy<Map.Entry<MetadataVersion, Int>> { it.value }
        .thenBy { it.key.major }
        .thenBy { it.key.minor }
    )
    return if (
      dominant != null &&
      size >= MIN_DOMINANT_SAMPLE_SIZE &&
      dominant.value.toDouble() / size >= MIN_DOMINANT_SHARE
    ) {
      setOf(dominant.key)
    } else {
      counts.keys
    }
  }

  private fun readKotlinModuleVersions(zip: IZipFile): Set<MetadataVersion> {
    return zip.getZipEntries().asSequence()
      .filter { entry -> entry.isDirectory.not() && entry.name.isKotlinModuleMetadataEntry() }
      .mapNotNull { entry ->
        runCatching {
          DataInputStream(BufferedInputStream(zip.getInputStream(entry))).use { input ->
            val componentCount = input.readInt()
            if (componentCount !in MIN_VERSION_COMPONENTS..MAX_VERSION_COMPONENTS) {
              return@runCatching null
            }
            List(componentCount) { input.readInt() }.toMetadataVersion()
          }
        }.getOrNull()
      }
      .toSet()
  }

  private fun Collection<MetadataVersion>.toDisplayString(): String {
    return sortedWith(
      compareByDescending<MetadataVersion> { it.major }
        .thenByDescending { it.minor }
    ).joinToString(" / ") { version ->
      "${version.major}.${version.minor}.x"
    }
  }

  private fun List<Int>.toMetadataVersion(): MetadataVersion? {
    if (size < MIN_VERSION_COMPONENTS) return null
    val major = this[0]
    val minor = this[1]
    if (major !in MIN_VERSION_NUMBER..MAX_VERSION_NUMBER || minor !in 0..MAX_VERSION_NUMBER) {
      return null
    }
    return MetadataVersion(major, minor)
  }

  private fun String.isKotlinModuleMetadataEntry(): Boolean {
    return startsWith(KOTLIN_MODULE_DIRECTORY) && endsWith(KOTLIN_MODULE_SUFFIX)
  }

  private fun String.isInNamespace(namespace: String): Boolean {
    return this == namespace || startsWith("$namespace.")
  }

  private fun String.isUsefulNamespace(): Boolean {
    return isNotBlank() && count { character -> character == '.' } >= MIN_NAMESPACE_DOTS
  }

  private fun String.toClassName(): String {
    return removePrefix("L").removeSuffix(";").replace('/', '.')
  }

  private data class ClassMetadataVersion(
    val className: String,
    val version: MetadataVersion
  )

  private data class ScopedMetadataVersions(
    val versions: Set<MetadataVersion>,
    val source: KotlinVersionSource
  )

  private data class MetadataVersion(
    val major: Int,
    val minor: Int
  )

  private const val KOTLIN_TOOLING_METADATA_ENTRY = "kotlin-tooling-metadata.json"
  private const val KOTLIN_ANDROID_TARGET = "org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget"
  private const val KOTLIN_ANDROID_PLUGIN = "org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper"
  private const val GRADLE_BUILD_SYSTEM = "Gradle"
  private const val KOTLIN_METADATA_ANNOTATION = "Lkotlin/Metadata;"
  private const val KOTLIN_METADATA_VERSION_ELEMENT = "mv"
  private const val KOTLIN_MODULE_DIRECTORY = "META-INF/"
  private const val KOTLIN_MODULE_SUFFIX = ".kotlin_module"
  private const val MIN_VERSION_COMPONENTS = 2
  private const val MAX_VERSION_COMPONENTS = 16
  private const val MIN_VERSION_NUMBER = 1
  private const val MAX_VERSION_NUMBER = 99
  private const val MIN_DOMINANT_SAMPLE_SIZE = 3
  private const val MIN_DOMINANT_SHARE = 2.0 / 3.0
  private const val MIN_NAMESPACE_DOTS = 2
}
