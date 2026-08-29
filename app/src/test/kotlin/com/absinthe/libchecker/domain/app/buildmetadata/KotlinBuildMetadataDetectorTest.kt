package com.absinthe.libchecker.domain.app.buildmetadata

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ServiceInfo
import com.absinthe.libchecker.compat.ZipFileCompat
import com.android.tools.smali.dexlib2.AnnotationVisibility
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.immutable.ImmutableAnnotation
import com.android.tools.smali.dexlib2.immutable.ImmutableAnnotationElement
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.android.tools.smali.dexlib2.immutable.value.ImmutableArrayEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableIntEncodedValue
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class KotlinBuildMetadataDetectorTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun prefersExactToolingPluginVersionAndKeepsBuildDetails() {
    val apk = createApk(
      toolingMetadata = TOOLING_METADATA,
      dexMetadataVersions = listOf(listOf(1, 9, 0))
    )

    val result = detect(apk)

    assertEquals("2.3.20", result.kotlinVersion)
    assertEquals(KotlinVersionSource.TOOLING_METADATA, result.kotlinVersionSource)
    assertEquals("8.14", result.gradleVersion)
    assertEquals("17", result.javaVersion)
  }

  @Test
  fun rejectsApkWideDexVersionWhenMoreThanOneCandidateRemains() {
    val apk = createApk(
      dexMetadataVersions = listOf(
        listOf(1, 9, 0),
        listOf(2, 1, 0),
        listOf(2, 1, 20)
      )
    )

    val result = detect(apk)

    assertEquals(null, result.kotlinVersion)
    assertEquals(null, result.kotlinVersionSource)
  }

  @Test
  fun prefersManifestEntryPointVersionOverBundledLibraryVersions() {
    val apk = createApk(
      dexMetadataClasses = listOf(
        DexMetadataFixture("com.example.app.MainActivity", listOf(2, 0, 0)),
        DexMetadataFixture("com.example.app.Feature", listOf(1, 9, 0)),
        DexMetadataFixture("com.vendor.Library", listOf(1, 7, 0))
      )
    )

    val result = detect(
      apk,
      KotlinVersionInferenceHints(
        packageName = "com.example.app",
        entryPointClassNames = setOf("com.example.app.MainActivity")
      )
    )

    assertEquals("2.0.x", result.kotlinVersion)
    assertEquals(KotlinVersionSource.DEX_ENTRY_POINTS, result.kotlinVersionSource)
  }

  @Test
  fun prefersAppNamespaceEntryPointsOverMergedLibraryComponents() {
    val apk = createApk(
      dexMetadataClasses = listOf(
        DexMetadataFixture("com.example.app.MainActivity", listOf(2, 0, 0)),
        DexMetadataFixture("androidx.startup.InitializationProvider", listOf(1, 7, 0))
      )
    )

    val result = detect(
      apk,
      KotlinVersionInferenceHints(
        packageName = "com.example.app",
        entryPointClassNames = setOf(
          "com.example.app.MainActivity",
          "androidx.startup.InitializationProvider"
        )
      )
    )

    assertEquals("2.0.x", result.kotlinVersion)
    assertEquals(KotlinVersionSource.DEX_ENTRY_POINTS, result.kotlinVersionSource)
  }

  @Test
  fun rejectsManifestEntryPointVersionsWhenMoreThanOneCandidateRemains() {
    val apk = createApk(
      dexMetadataClasses = listOf(
        DexMetadataFixture("com.example.app.MainActivity", listOf(2, 0, 0)),
        DexMetadataFixture("com.example.app.SyncService", listOf(1, 9, 0))
      )
    )

    val result = detect(
      apk,
      KotlinVersionInferenceHints(
        packageName = "com.example.app",
        entryPointClassNames = setOf(
          "com.example.app.MainActivity",
          "com.example.app.SyncService"
        )
      )
    )

    assertEquals(null, result.kotlinVersion)
    assertEquals(null, result.kotlinVersionSource)
  }

  @Test
  fun usesDominantAppNamespaceVersionWhenEntryPointsHaveNoKotlinMetadata() {
    val apk = createApk(
      dexMetadataClasses = listOf(
        DexMetadataFixture("com.example.app.FeatureA", listOf(2, 1, 0)),
        DexMetadataFixture("com.example.app.FeatureB", listOf(2, 1, 0)),
        DexMetadataFixture("com.example.app.FeatureC", listOf(2, 1, 0)),
        DexMetadataFixture("com.example.app.LegacyFeature", listOf(1, 9, 0)),
        DexMetadataFixture("com.vendor.Library", listOf(1, 7, 0))
      )
    )

    val result = detect(
      apk,
      KotlinVersionInferenceHints(
        packageName = "com.example.app",
        entryPointClassNames = setOf("com.example.app.JavaActivity")
      )
    )

    assertEquals("2.1.x", result.kotlinVersion)
    assertEquals(KotlinVersionSource.DEX_APP_NAMESPACE, result.kotlinVersionSource)
  }

  @Test
  fun rejectsAppNamespaceVersionsWhenNoSingleDominantCandidateRemains() {
    val apk = createApk(
      dexMetadataClasses = listOf(
        DexMetadataFixture("com.example.app.FeatureA", listOf(2, 1, 0)),
        DexMetadataFixture("com.example.app.FeatureB", listOf(2, 1, 0)),
        DexMetadataFixture("com.example.app.LegacyA", listOf(1, 9, 0)),
        DexMetadataFixture("com.example.app.LegacyB", listOf(1, 9, 0))
      )
    )

    val result = detect(
      apk,
      KotlinVersionInferenceHints(
        packageName = "com.example.app",
        entryPointClassNames = emptySet()
      )
    )

    assertEquals(null, result.kotlinVersion)
    assertEquals(null, result.kotlinVersionSource)
  }

  @Test
  fun buildsInferenceHintsFromQualifiedAndRelativeManifestEntryPoints() {
    val packageInfo = PackageInfo().apply {
      packageName = "com.example.app"
      applicationInfo = ApplicationInfo().apply { className = ".App" }
      activities = arrayOf(ActivityInfo().apply { name = "com.example.app.MainActivity" })
      services = arrayOf(ServiceInfo().apply { name = "SyncService" })
    }

    val hints = KotlinVersionInferenceHints.from(packageInfo)

    assertEquals("com.example.app", hints.packageName)
    assertEquals(
      setOf(
        "com.example.app.App",
        "com.example.app.MainActivity",
        "com.example.app.SyncService"
      ),
      hints.entryPointClassNames
    )
  }

  @Test
  fun buildsInferenceHintsUsingSeparatelyLoadedComponentPackageInfo() {
    val packageInfo = PackageInfo().apply {
      packageName = "com.example.app"
      applicationInfo = ApplicationInfo()
    }
    val componentPackageInfo = PackageInfo().apply {
      packageName = "com.example.app"
      activities = arrayOf(ActivityInfo().apply { name = ".MainActivity" })
      services = arrayOf(ServiceInfo().apply { name = "SyncService" })
    }

    val hints = KotlinVersionInferenceHints.from(packageInfo, componentPackageInfo)

    assertEquals(
      setOf("com.example.app.MainActivity", "com.example.app.SyncService"),
      hints.entryPointClassNames
    )
  }

  @Test
  fun fallsBackToKotlinModuleMetadataWhenDexAnnotationsAreAbsent() {
    val apk = createApk(kotlinModuleVersion = listOf(2, 0, 0))

    val result = detect(apk)

    assertEquals("2.0.x", result.kotlinVersion)
    assertEquals(KotlinVersionSource.KOTLIN_MODULE, result.kotlinVersionSource)
  }

  private fun detect(
    apk: File,
    inferenceHints: KotlinVersionInferenceHints? = null
  ): KotlinBuildMetadata {
    return ZipFileCompat(apk).use { zip ->
      KotlinBuildMetadataDetector.detect(apk, zip, inferenceHints = inferenceHints)
    }
  }

  private fun createApk(
    toolingMetadata: String? = null,
    dexMetadataVersions: List<List<Int>> = emptyList(),
    dexMetadataClasses: List<DexMetadataFixture> = emptyList(),
    kotlinModuleVersion: List<Int>? = null
  ): File {
    return temporaryFolder.newFile("fixture-${System.nanoTime()}.apk").apply {
      outputStream().use { output ->
        ZipOutputStream(output).use { zip ->
          toolingMetadata?.let { metadata ->
            zip.putNextEntry(ZipEntry("kotlin-tooling-metadata.json"))
            zip.write(metadata.toByteArray())
            zip.closeEntry()
          }
          val dexFixtures = dexMetadataClasses.ifEmpty {
            dexMetadataVersions.mapIndexed { index, version ->
              DexMetadataFixture("fixture.C$index", version)
            }
          }
          if (dexFixtures.isNotEmpty()) {
            zip.putNextEntry(ZipEntry("classes.dex"))
            zip.write(createDex(dexFixtures))
            zip.closeEntry()
          }
          kotlinModuleVersion?.let { version ->
            zip.putNextEntry(ZipEntry("META-INF/app.kotlin_module"))
            zip.write(createKotlinModuleHeader(version))
            zip.closeEntry()
          }
        }
      }
    }
  }

  private fun createDex(fixtures: List<DexMetadataFixture>): ByteArray {
    val dexFile = temporaryFolder.newFile()
    val classes = fixtures.map { fixture ->
      val metadataAnnotation = ImmutableAnnotation(
        AnnotationVisibility.RUNTIME,
        "Lkotlin/Metadata;",
        setOf(
          ImmutableAnnotationElement(
            "mv",
            ImmutableArrayEncodedValue(fixture.metadataVersion.map(::ImmutableIntEncodedValue))
          )
        )
      )
      ImmutableClassDef(
        "L${fixture.className.replace('.', '/')};",
        0x1,
        "Ljava/lang/Object;",
        null,
        null,
        setOf(metadataAnnotation),
        null,
        null
      )
    }
    DexPool.writeTo(dexFile.path, ImmutableDexFile(Opcodes.getDefault(), classes))
    return dexFile.readBytes()
  }

  private data class DexMetadataFixture(
    val className: String,
    val metadataVersion: List<Int>
  )

  private fun createKotlinModuleHeader(version: List<Int>): ByteArray {
    return ByteArrayOutputStream().use { output ->
      DataOutputStream(output).use { data ->
        data.writeInt(version.size)
        version.forEach(data::writeInt)
      }
      output.toByteArray()
    }
  }

  private companion object {
    val TOOLING_METADATA = """
      {
        "buildSystem": "Gradle",
        "buildSystemVersion": "8.14",
        "buildPlugin": "org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper",
        "buildPluginVersion": "2.3.20",
        "projectTargets": [
          {
            "target": "org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget",
            "platformType": "androidJvm",
            "extras": {
              "android": {
                "sourceCompatibility": "17",
                "targetCompatibility": "17"
              }
            }
          }
        ]
      }
    """.trimIndent()
  }
}
