import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

fun Project.includeKotlinToolingMetadataInApk() {
  plugins.withId("com.android.application") {
    val metadataTask = getKotlinToolingMetadataTaskProvider()
    val androidComponents = extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

    androidComponents.onVariants { variant ->
      if (!variant.debuggable) {
        val capName = variant.name.replaceFirstChar { it.uppercaseChar() }

        val appendTask = tasks.register(
          "include${capName}KotlinToolingMetadataInApk",
          CopyKotlinToolingMetadataForApkTask::class.java
        ) {
          dependsOn(metadataTask)
          inputFile.set(layout.buildDirectory.file("kotlinToolingMetadata/kotlin-tooling-metadata.json"))
        }

        variant.artifacts
          .forScope(ScopedArtifacts.Scope.PROJECT)
          .use(appendTask)
          .toAppend(ScopedArtifact.JAVA_RES) { it.outputDirectory }
      }
    }
  }
}

@Suppress("UNCHECKED_CAST")
private fun Project.getKotlinToolingMetadataTaskProvider(): TaskProvider<out Task> {
  val holder = Class.forName("org.jetbrains.kotlin.gradle.tooling.BuildKotlinToolingMetadataTaskKt")
  val getter = holder.getDeclaredMethod("getBuildKotlinToolingMetadataTask", Project::class.java)
  getter.isAccessible = true
  return getter.invoke(null, this) as TaskProvider<out Task>
}

abstract class CopyKotlinToolingMetadataForApkTask : DefaultTask() {
  @get:InputFile
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val inputFile: RegularFileProperty

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  @TaskAction
  fun run() {
    val input = inputFile.get().asFile
    val outputDir = outputDirectory.get().asFile
    outputDir.mkdirs()
    if (!input.isFile) {
      return
    }
    val output = outputDirectory.file("kotlin-tooling-metadata.json").get().asFile
    input.copyTo(output, overwrite = true)
  }
}
