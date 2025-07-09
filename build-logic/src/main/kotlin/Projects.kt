import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import java.io.File
import org.gradle.api.Project

const val baseVersionName = "2.5.2"
val Project.verName: String get() = "${baseVersionName}${versionNameSuffix}.${exec("git rev-parse --short=7 HEAD")}"
val Project.verCode: Int get() = exec("git rev-list --count HEAD").toInt()
val Project.isDevVersion: Boolean get() = exec("git tag -l $baseVersionName").isEmpty()
val Project.versionNameSuffix: String get() = if (isDevVersion) ".dev" else ""

fun Project.setupLibraryModule(block: LibraryExtension.() -> Unit = {}) {
  setupBaseModule(block)
}

fun Project.setupAppModule(block: BaseAppModuleExtension.() -> Unit = {}) {
  setupBaseModule<BaseAppModuleExtension> {
    defaultConfig {
      versionCode = verCode
      versionName = verName
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
    val releaseSigning = if (project.hasProperty("releaseStoreFile")) {
      signingConfigs.create("release") {
        storeFile = File(project.properties["releaseStoreFile"] as String)
        storePassword = project.properties["releaseStorePassword"] as String
        keyAlias = project.properties["releaseKeyAlias"] as String
        keyPassword = project.properties["releaseKeyPassword"] as String
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
        proguardFiles(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          "proguard-rules.pro"
        )
      }
      all {
        signingConfig = releaseSigning
        buildConfigField("Boolean", "IS_DEV_VERSION", isDevVersion.toString())
        //buildConfigField("String", "BUILD_TIME", "\"" + Instant.now().toString() + "\"")
      }
    }

    block()
  }
}

private inline fun <reified T : BaseExtension> Project.setupBaseModule(crossinline block: T.() -> Unit = {}) {
  extensions.configure<BaseExtension>("android") {
    compileSdkVersion(36)
    defaultConfig {
      minSdk = 24
      targetSdk = 36
    }
    sourceSets.configureEach {
      java.srcDirs("src/$name/kotlin")
    }
    (this as T).block()
  }
}

fun Project.exec(command: String): String = providers.exec {
  commandLine(command.split(" "))
}.standardOutput.asText.get().trim()
