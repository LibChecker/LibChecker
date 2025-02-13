import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.parcelize) apply false
  alias(libs.plugins.protobuf) apply false
  alias(libs.plugins.hiddenApiRefine) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.moshiX) apply false
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.aboutlibraries) apply false
  alias(libs.plugins.gms) apply false
  alias(libs.plugins.firebase.crashlytics) apply false
  id("build-logic") apply false
}

allprojects {
  plugins.apply(rootProject.libs.plugins.spotless.get().pluginId)
  extensions.configure<SpotlessExtension> {
    kotlin {
      target("src/**/*.kt")
      ktlint(rootProject.libs.ktlint.get().version)
    }
    kotlinGradle {
      ktlint(rootProject.libs.ktlint.get().version)
    }
  }

  // Configure Java to use our chosen language level. Kotlin will automatically pick this up.
  // See https://kotlinlang.org/docs/gradle-configure-project.html#gradle-java-toolchains-support
  plugins.withType<JavaBasePlugin>().configureEach {
    extensions.configure<JavaPluginExtension> {
      toolchain.languageVersion = JavaLanguageVersion.of(21)
    }
  }
}
