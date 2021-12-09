buildscript {
  apply("gradle/extra.gradle.kts")

  repositories {
    google()
    gradlePluginPortal()
  }

  dependencies {
    classpath(rootProject.extra["androidPlugin"].toString())
    classpath(rootProject.extra["kotlinPlugin"].toString())
    classpath(Libs.protobufPlugin)
    classpath(Libs.kotlinterPlugin)
    classpath(Libs.hiddenApiRefinePlugin)
    classpath(Libs.kspPlugin)
  }
}

allprojects {
  apply("$rootDir/gradle/extra.gradle.kts")

  apply(plugin = "org.jmailen.kotlinter")
}

task<Delete>("clean") {
  delete(rootProject.buildDir)
}
