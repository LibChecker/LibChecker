import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete

class CleanAboutLibrariesPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val cleanTask = project.tasks.register(
      "cleanAboutLibrariesGenerated",
      Delete::class.java
    ) {
      println("cleanAboutLibrariesGenerated")
      delete(project.layout.buildDirectory.dir("generated/aboutLibraries"))
    }

    project.tasks.configureEach {
      if (name == "preBuild") {
        dependsOn(cleanTask)
      }
    }
  }
}
