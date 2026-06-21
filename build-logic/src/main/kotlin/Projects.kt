import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import java.io.File
import org.gradle.api.Project

const val baseVersionName = "2.5.5"

fun Project.setupLibraryModule(block: LibraryExtension.() -> Unit = {}) {
  setupBaseModule(block)
}

fun Project.setupAppModule(block: ApplicationExtension.() -> Unit = {}) {
  val projectVersion = resolveProjectVersion()

  setupBaseModule<ApplicationExtension> {
    defaultConfig {
      versionCode = projectVersion.code
      versionName = projectVersion.name
      minSdk = 24
      targetSdk = 37

      androidResources {
        ignoreAssetsPatterns += "!PublicSuffixDatabase.list" // OkHttp5
        generateLocaleConfig = true
        localeFilters += mutableSetOf(
          "en",
          "ar-rSA",
          "de-rDE",
          "in-rID",
          "iw-rIL",
          "ja-rJP",
          "pt-rBR",
          "ru-rRU",
          "tr-rTR",
          "uk-rUA",
          "vi-rVN",
          "zh-rCN",
          "zh-rTW",
          "zh-rHK",
        )
      }
    }
    val releaseStoreFile = providers.gradleProperty("releaseStoreFile")
    val releaseSigning = if (releaseStoreFile.isPresent) {
      signingConfigs.create("release") {
        storeFile = File(releaseStoreFile.get())
        storePassword = providers.gradleProperty("releaseStorePassword").get()
        keyAlias = providers.gradleProperty("releaseKeyAlias").get()
        keyPassword = providers.gradleProperty("releaseKeyPassword").get()
      }
    } else {
      signingConfigs.getByName("debug")
    }
    buildTypes {
      debug {
        applicationIdSuffix = ".debug"
      }
      release {
        isMinifyEnabled = true
        isShrinkResources = true
      }
      all {
        signingConfig = releaseSigning
        buildConfigField("Boolean", "IS_DEV_VERSION", projectVersion.isDev.toString())
        //buildConfigField("String", "BUILD_TIME", "\"" + Instant.now().toString() + "\"")
      }
    }

    block()
  }
}

private inline fun <reified T : CommonExtension> Project.setupBaseModule(crossinline block: T.() -> Unit = {}) {
  extensions.configure<CommonExtension>("android") {
    compileSdk = 37
    compileSdkMinor = 0

    sourceSets.configureEach {
      java.directories.add("src/$name/kotlin")
    }
    includeKotlinToolingMetadataInApk()
    (this as T).block()
  }
}

private data class ProjectVersion(
  val name: String,
  val code: Int,
  val isDev: Boolean
)

private fun Project.resolveProjectVersion(): ProjectVersion {
  val isDev = exec("git", "tag", "-l", baseVersionName).isEmpty()
  val suffix = if (isDev) ".dev" else ""
  val commit = exec("git", "rev-parse", "--short=7", "HEAD")
  val code = exec("git", "rev-list", "--count", "HEAD").toInt()

  return ProjectVersion(
    name = "$baseVersionName$suffix.$commit",
    code = code,
    isDev = isDev
  )
}

private fun Project.exec(vararg command: String): String = providers.exec {
  commandLine(*command)
}.standardOutput.asText.get().trim()
