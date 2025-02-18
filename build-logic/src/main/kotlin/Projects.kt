import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import java.io.File
import java.time.Instant
import org.gradle.api.Project

const val baseVersionName = "2.5.1"
val Project.verName: String get() = "${baseVersionName}${versionNameSuffix}.${exec("git rev-parse --short HEAD")}"
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
      resourceConfigurations += arrayOf(
        "en",
        "zh-rCN",
        "zh-rTW",
        "zh-rHK",
        "ru-rRU",
        "ru-rUA",
        "ja-rJP",
        "vi-rVN",
        "in-rID",
        "pt-rBR",
        "ar-rSA",
        "tr-rTR",
      )
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
    compileSdkVersion(35)
    defaultConfig {
      minSdk = 24
      targetSdk = 35
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
