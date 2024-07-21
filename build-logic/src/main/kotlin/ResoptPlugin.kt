import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.api.AndroidBasePlugin
import java.nio.file.Paths
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileSystemOperations
import org.gradle.kotlin.dsl.newInstance
import org.gradle.process.ExecOperations

interface Injected {
  @get:Inject
  val fs: FileSystemOperations

  @get:Inject
  val exec: ExecOperations
}

class ResoptPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.plugins.withType(AndroidBasePlugin::class.java) {
      project.extensions.configure(AndroidComponentsExtension::class.java) {
        onVariants { variant ->
          if (variant.buildType != "release") return@onVariants
          val name = variant.name
          val ext = project.extensions.getByType(CommonExtension::class.java)
          val aapt2 = Paths.get(
            sdkComponents.sdkDirectory.get().toString(), "build-tools", ext.buildToolsVersion, "aapt2"
          )
          val workdir = Paths.get(
            project.layout.buildDirectory.toString(), "intermediates", "optimized_processed_res", name, "optimize${name.replaceFirstChar { it.uppercase() }}Resources"
          ).toFile()
          val zip =
            if (variant.flavorName.isNullOrEmpty()) "resources-${variant.buildType}-optimize.ap_" else "resources-${variant.flavorName}-${variant.buildType}-optimize.ap_"
          val optimized = "$zip.opt"
          project.afterEvaluate {
            val injected = objects.newInstance<Injected>()
            tasks.getByPath("optimize${name.replaceFirstChar { c -> c.uppercase() }}Resources").doLast {
              val cmd = injected.exec.exec {
                commandLine(
                  aapt2,
                  "optimize",
                  "--collapse-resource-names",
                  "-o",
                  optimized,
                  zip
                )
                workingDir = workdir
                isIgnoreExitValue = true
              }
              if (cmd.exitValue == 0) {
                injected.fs.copy {
                  from(workdir.resolve(optimized))
                  rename { zip }
                  into(workdir)
                }
                injected.fs.delete {
                  delete(workdir.resolve(optimized))
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
}
