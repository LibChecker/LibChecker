import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import java.io.File
import java.nio.charset.Charset
import java.time.Instant
import org.gradle.api.JavaVersion
import org.gradle.api.Project

const val baseVersionName = "2.3.9"
val verName: String by lazy { "${baseVersionName}${versionNameSuffix}.${"git rev-parse --short HEAD".exec()}" }
val verCode: Int by lazy { "git rev-list --count HEAD".exec().toInt() }
val isDevVersion: Boolean by lazy { "git tag -l $baseVersionName".exec().isEmpty() }
val versionNameSuffix = if (isDevVersion) ".dev" else ""
val javaLevel = JavaVersion.VERSION_11

fun Project.setupLibraryModule(block: LibraryExtension.() -> Unit = {}) {
  setupBaseModule(block)
}

fun Project.setupAppModule(block: BaseAppModuleExtension.() -> Unit = {}) {
  setupBaseModule<BaseAppModuleExtension> {
    defaultConfig {
      versionCode = verCode
      versionName = verName
      resourceConfigurations += arrayOf("en", "zh-rCN", "zh-rTW", "zh-rHK", "ru-rRU", "ru-rUA")
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
        buildConfigField(
          "String",
          "APP_CENTER_SECRET",
          "\"" + System.getenv("APP_CENTER_SECRET").orEmpty() + "\""
        )
        buildConfigField("String", "BUILD_TIME", "\"" + Instant.now().toString() + "\"")
      }
    }

    block()
  }
}

private inline fun <reified T : BaseExtension> Project.setupBaseModule(crossinline block: T.() -> Unit = {}) {
  extensions.configure<BaseExtension>("android") {
    compileSdkVersion(33)
    defaultConfig {
      minSdk = 24
      targetSdk = 33
    }
    sourceSets.configureEach {
      java.srcDirs("src/$name/kotlin")
    }
    compileOptions {
      targetCompatibility(javaLevel)
      sourceCompatibility(javaLevel)
    }
    (this as T).block()
  }
}

fun String.exec(): String {
  val process = ProcessBuilder(*this.split("\\s".toRegex()).toTypedArray())
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .start()
  val result = process.inputStream.readAllBytes().toString(Charset.defaultCharset()).trim()
  process.waitFor()
  return result
}

