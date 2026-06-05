import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import java.nio.file.Paths
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

        project.tasks.matching { it.name == optimizeTaskName }.configureEach {
          doLast {
            val workdirFile = workdir.get().asFile
            val cmd = injected.exec.exec {
              commandLine(
                aapt2.get(),
                "optimize",
                "--collapse-resource-names",
                "--resources-config-path", cfg.asFile.path,
                "-o",
                optimized,
                zip
              )
              workingDir = workdirFile
              isIgnoreExitValue = true
            }
            if (cmd.exitValue == 0) {
              injected.fs.copy {
                from(workdirFile.resolve(optimized))
                rename { zip }
                into(workdirFile)
              }
              injected.fs.delete {
                delete(workdirFile.resolve(optimized))
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
