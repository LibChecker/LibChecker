plugins {
  `kotlin-dsl`
}

gradlePlugin {
  plugins {
    create("build-logic") {
      id = "build-logic"
      implementationClass = "BuildLogic"
    }
    create("res-opt") {
      id = "res-opt"
      implementationClass = "ResoptPlugin"
    }
    create("stable-manifest") {
      id = "stable-manifest"
      implementationClass = "StableManifestPlugin"
    }
  }
}

dependencies {
  implementation(libs.gradlePlugin.android)
  implementation(libs.gradlePlugin.aapt2Proto)
  implementation(libs.gradlePlugin.protobuf)
  implementation(libs.gradlePlugin.kotlin)
  implementation(libs.gradlePlugin.spotless)
}
