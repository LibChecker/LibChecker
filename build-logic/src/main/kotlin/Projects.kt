import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import java.io.File
import org.gradle.api.Project

const val baseVersionName = "2.5.4"
val Project.verName: String get() = "${baseVersionName}${versionNameSuffix}.${exec("git rev-parse --short=7 HEAD")}"
val Project.verCode: Int get() = exec("git rev-list --count HEAD").toInt()
val Project.isDevVersion: Boolean get() = exec("git tag -l $baseVersionName").isEmpty()
val Project.versionNameSuffix: String get() = if (isDevVersion) ".dev" else ""

fun Project.setupLibraryModule(block: LibraryExtension.() -> Unit = {}) {
  setupBaseModule(block)
}

fun Project.setupAppModule(block: ApplicationExtension.() -> Unit = {}) {
  setupBaseModule<ApplicationExtension> {
    defaultConfig {
      versionCode = verCode
      versionName = verName
      minSdk = 24
      targetSdk = 36

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

private inline fun <reified T : CommonExtension> Project.setupBaseModule(crossinline block: T.() -> Unit = {}) {
  extensions.configure<CommonExtension>("android") {
    compileSdk = 36

    sourceSets.configureEach {
      java.directories.add("src/$name/kotlin")
    }
    includeKotlinToolingMetadataInApk()
    (this as T).block()
  }
}

fun Project.exec(command: String): String = providers.exec {
  commandLine(command.split(" "))
}.standardOutput.asText.get().trim()

fun Project.includeKotlinToolingMetadataInApk() {
  val m = Class.forName("org.jetbrains.kotlin.gradle.tooling.IncludeKotlinToolingMetadataInApkKt")
    .getDeclaredMethod("includeKotlinToolingMetadataInApk", Project::class.java)
  m.isAccessible = true
  m.invoke(null, this)
}
