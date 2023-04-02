plugins {
  `kotlin-dsl`
}

repositories {
  google()
  mavenCentral()
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
  }
}

dependencies {
  implementation(libs.gradlePlugin.android)
  implementation(libs.gradlePlugin.kotlin)
}
