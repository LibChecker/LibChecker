import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import java.nio.charset.Charset

const val baseVersionName = "2.1.9"
val verName: String by lazy { "${baseVersionName}.${"git rev-parse --short HEAD".exec()}" }
val verCode: Int by lazy { "git rev-list --count HEAD".exec().toInt() }

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
    }
    block()
  }
}

private inline fun <reified T : BaseExtension> Project.setupBaseModule(crossinline block: T.() -> Unit = {}) {
  extensions.configure<BaseExtension>("android") {
    compileSdkVersion(31)
    buildToolsVersion = "31.0.0"
    defaultConfig {
      minSdk = 23
      targetSdk = 31
      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    kotlinOptions {
      jvmTarget = JavaVersion.VERSION_11.toString()
    }
    compileOptions {
      targetCompatibility(JavaVersion.VERSION_11)
      sourceCompatibility(JavaVersion.VERSION_11)
    }
    dependencies {
      implementations(
        fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))),
        Libs.coroutines,
        Libs.appCompat,
        Libs.core
      )

      testImplementations(*Libs.tests)
      androidTestImplementations(*Libs.androidTests)
    }
    (this as T).block()
  }
}

private fun BaseExtension.kotlinOptions(block: KotlinJvmOptions.() -> Unit) {
  (this as ExtensionAware).extensions.configure("kotlinOptions", block)
}

fun String.exec(): String = Runtime.getRuntime().exec(this).inputStream.readBytes()
  .toString(Charset.defaultCharset()).trim()

fun DependencyHandler.implementations(vararg names: Any): Array<Dependency?> =
  config("implementation", *names)

fun DependencyHandler.androidTestImplementations(vararg names: Any): Array<Dependency?> =
  config("androidTestImplementation", *names)

fun DependencyHandler.testImplementations(vararg names: Any): Array<Dependency?> =
  config("testImplementation", *names)

private fun DependencyHandler.config(operation: String, vararg names: Any): Array<Dependency?> =
  names.map { add(operation, it) }.toTypedArray()
