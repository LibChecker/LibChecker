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
  }
}

dependencies {
  implementation(libs.gradlePlugin.android)
  implementation(libs.gradlePlugin.kotlin)
}
