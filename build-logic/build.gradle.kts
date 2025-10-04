plugins {
  `kotlin-dsl`
}

repositories {
  google {
    content {
      includeGroupByRegex(".*google.*")
      includeGroupByRegex(".*android.*")
    }
  }
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
    create("clean-aboutlibraries") {
      id = "clean-aboutlibraries"
      implementationClass = "CleanAboutLibrariesPlugin"
    }
  }
}

dependencies {
  implementation(libs.gradlePlugin.android)
  implementation(libs.gradlePlugin.kotlin)
}
