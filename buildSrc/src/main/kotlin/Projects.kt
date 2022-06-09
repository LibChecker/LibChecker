import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import java.nio.charset.Charset

const val baseVersionName = "2.2.9"
val verName: String by lazy { "${baseVersionName}${versionNameSuffix}.${"git rev-parse --short HEAD".exec()}" }
val verCode: Int by lazy { "git rev-list --count HEAD".exec().toInt() }
val isDevVersion: Boolean by lazy { "git tag -l $baseVersionName".exec().isEmpty() }
val versionNameSuffix = if (isDevVersion) ".dev" else ""

fun Project.setupLibraryModule(block: LibraryExtension.() -> Unit = {}) {
  setupBaseModule(block)
}

fun Project.setupAppModule(block: BaseAppModuleExtension.() -> Unit = {}) {
  setupBaseModule<BaseAppModuleExtension> {
    defaultConfig {
      versionCode = verCode
      versionName = verName
      resourceConfigurations += arrayOf("en", "zh-rCN", "zh-rTW", "ru", "uk-rUA")
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
        buildConfigField("Boolean", "IS_DEV_VERSION", isDevVersion.toString())
      }
    }

    productFlavors {
      flavorDimensions += "channel"

      create("foss") {
        isDefault = true
        dimension = flavorDimensionList[0]
      }
      create("market") {
        dimension = flavorDimensionList[0]
      }
      all {
        manifestPlaceholders["channel"] = this.name
      }
    }

    block()
  }
}

private inline fun <reified T : BaseExtension> Project.setupBaseModule(crossinline block: T.() -> Unit = {}) {
  extensions.configure<BaseExtension>("android") {
    compileSdkVersion(33)
    defaultConfig {
      minSdk = 23
      targetSdk = 33
      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    kotlinOptions {
      jvmTarget = JavaVersion.VERSION_11.toString()
      freeCompilerArgs = freeCompilerArgs + arrayOf("-opt-in=kotlin.RequiresOptIn")
    }
    compileOptions {
      targetCompatibility(JavaVersion.VERSION_11)
      sourceCompatibility(JavaVersion.VERSION_11)
    }
    (this as T).block()
  }
}

private fun BaseExtension.kotlinOptions(block: KotlinJvmOptions.() -> Unit) {
  (this as ExtensionAware).extensions.configure("kotlinOptions", block)
}

fun String.exec(): String = Runtime.getRuntime().exec(this).inputStream.readBytes()
  .toString(Charset.defaultCharset()).trim()
