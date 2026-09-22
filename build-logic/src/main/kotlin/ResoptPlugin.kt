import com.android.aapt.ConfigurationOuterClass.Configuration
import com.android.aapt.Resources.ResourceTable
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import java.io.File
import java.nio.file.Paths
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileSystemOperations
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.newInstance
import org.gradle.process.ExecOperations

interface Injected {
  @get:Inject
  val fs: FileSystemOperations

  @get:Inject
  val exec: ExecOperations
}

class ResoptPlugin : Plugin<Project> {
  @Suppress("NewApi")
  override fun apply(project: Project) {
    project.plugins.withId("com.android.application") {
      val androidComponents = project.extensions.getByType<ApplicationAndroidComponentsExtension>()
      val androidExtension = project.extensions.getByType(CommonExtension::class.java)
      val injected = project.objects.newInstance<Injected>()

      androidComponents.onVariants(androidComponents.selector().withBuildType("release")) { variant ->
        val name = variant.name
        val capName = name.replaceFirstChar { it.uppercase() }
        val optimizeTaskName = "optimize${capName}Resources"
        val aapt2 = androidComponents.sdkComponents.sdkDirectory.map {
          Paths.get(it.asFile.toString(), "build-tools", androidExtension.buildToolsVersion, "aapt2").toFile()
        }
        val workdir = project.layout.buildDirectory.dir(
          "intermediates/optimized_processed_res/$name/$optimizeTaskName"
        )
        val cfg = project.layout.projectDirectory.file("aapt2-resources.cfg")
        val zip =
          if (variant.flavorName.isNullOrEmpty()) {
            "resources-${variant.buildType}-optimize.ap_"
          } else {
            "resources-${variant.flavorName}-${variant.buildType}-optimize.ap_"
          }
        val optimized = "$zip.opt"
        val minSdk = variant.minSdk.apiLevel

        project.tasks.matching { it.name == optimizeTaskName }.configureEach {
          doLast {
            val workdirFile = workdir.get().asFile
            val original = workdirFile.resolve(zip)
            val proto = workdirFile.resolve("$zip.proto")
            val filtered = workdirFile.resolve("$zip.filtered")
            val binary = workdirFile.resolve("$zip.binary")
            injected.exec.exec {
              commandLine(aapt2.get(), "convert", "--output-format", "proto", "-o", proto, original)
            }
            val removed = removeRedundantRasters(proto, filtered, minSdk)
            val cmd = injected.exec.exec {
              commandLine(
                aapt2.get(),
                "convert",
                "--output-format", "binary",
                "--collapse-resource-names",
                "--deduplicate-entry-values",
                "--resources-config-path", cfg.asFile.path,
                "-o", binary,
                filtered
              )
              workingDir = workdirFile
              isIgnoreExitValue = true
            }
            if (cmd.exitValue == 0) {
              // Preserve all original binary XML, including manifest escaping. Only the
              // resource table and unreachable raster alternatives change in this pass.
              val table = ZipFile(binary).use { it.getInputStream(it.getEntry("resources.arsc")).readBytes() }
              rewriteArchive(original, workdirFile.resolve(optimized), removed, "resources.arsc", table)
              injected.fs.copy {
                from(workdirFile.resolve(optimized))
                rename { zip }
                into(workdirFile)
              }
              injected.fs.delete {
                delete(workdirFile.resolve(optimized), proto, filtered, binary)
              }
            } else {
              println("Failed to optimize $name resources")
            }
          }
        }
      }
    }
  }
}

private fun removeRedundantRasters(input: File, output: File, minSdk: Int): Set<String> {
  val table = ZipFile(input).use {
    ResourceTable.parseFrom(it.getInputStream(it.getEntry("resources.pb"))).toBuilder()
  }
  val removed = mutableSetOf<String>()
  for (pkg in table.packageBuilderList) {
    for (type in pkg.typeBuilderList) {
      if (type.name != "drawable" && type.name != "mipmap") continue
      for (entry in type.entryBuilderList) {
        // An unqualified anydpi resource wins over every concrete density on supported APIs.
        val hasVector = entry.configValueList.any {
          it.config.density == 0xfffe && it.config.sdkVersion <= minSdk &&
            it.config.onlyDensityAndVersion() && it.value.item.file.path.endsWith(".xml")
        }
        if (!hasVector) continue
        for (index in entry.configValueCount - 1 downTo 0) {
          val value = entry.getConfigValue(index)
          if (value.config.density in 1..0xfffd && value.config.sdkVersion <= minSdk &&
            value.config.onlyDensityAndVersion() && value.value.item.file.path.endsWith(".png")
          ) {
            removed += value.value.item.file.path
            entry.removeConfigValue(index)
          }
        }
      }
    }
  }
  // AAPT may share a file between entries. Keep it if any surviving value still uses it.
  for (pkg in table.packageList) {
    for (type in pkg.typeList) {
      for (entry in type.entryList) {
        for (value in entry.configValueList) removed -= value.value.item.file.path
      }
    }
  }
  rewriteArchive(input, output, removed, "resources.pb", table.build().toByteArray())
  println("Removed ${removed.size} raster files superseded by anydpi resources at minSdk $minSdk")
  return removed
}

private fun Configuration.onlyDensityAndVersion() =
  toBuilder().clearDensity().clearSdkVersion().build() == Configuration.getDefaultInstance()

private fun rewriteArchive(input: File, output: File, removed: Set<String>, replaced: String, data: ByteArray) {
  ZipFile(input).use { source ->
    ZipOutputStream(output.outputStream()).use { target ->
      target.setLevel(9)
      for (entry in source.entries()) {
        if (entry.name in removed) continue
        val bytes = if (entry.name == replaced) data else source.getInputStream(entry).use { it.readBytes() }
        target.putNextEntry(
          ZipEntry(entry.name).apply {
            time = entry.time
            method = entry.method
            size = bytes.size.toLong()
            crc = CRC32().apply { update(bytes) }.value
          }
        )
        target.write(bytes)
        target.closeEntry()
      }
    }
  }
}
