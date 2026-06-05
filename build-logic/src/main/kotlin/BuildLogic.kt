import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

class BuildLogic : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      val ktlintVersion = extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")
        .findVersion("ktlint")
        .get()
        .requiredVersion

      pluginManager.apply("com.diffplug.spotless")
      extensions.configure<SpotlessExtension> {
        kotlin {
          target("src/**/*.kt")
          ktlint(ktlintVersion)
        }
        kotlinGradle {
          ktlint(ktlintVersion)
        }
      }

      plugins.withType<JavaBasePlugin>().configureEach {
        extensions.configure<JavaPluginExtension> {
          toolchain.languageVersion.set(JavaLanguageVersion.of(25))
        }
      }

      tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
      }
    }
  }
}
