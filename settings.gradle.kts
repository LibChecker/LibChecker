pluginManagement {
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id == "dev.rikka.tools.refine.gradle-plugin") {
        useModule("dev.rikka.tools.refine:gradle-plugin:${requested.version}")
      }
    }
  }
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
  }
}

plugins {
  id("com.gradle.enterprise") version "3.11.1"
}

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
    publishAlways()
  }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app", ":hidden-api")
includeBuild("build-logic")

rootProject.name = "LibChecker"
