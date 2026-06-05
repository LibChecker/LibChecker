plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.parcelize) apply false
  alias(libs.plugins.protobuf) apply false
  alias(libs.plugins.hiddenApiRefine) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.moshiX) apply false
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.aboutlibraries) apply false
  alias(libs.plugins.gms) apply false
  alias(libs.plugins.firebase.crashlytics) apply false
  id("build-logic")
}
