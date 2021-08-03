import org.jmailen.gradle.kotlinter.KotlinterExtension

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
  }
}

allprojects {
  apply("$rootDir/gradle/extra.gradle.kts")

  apply(plugin = "org.jmailen.kotlinter")

  configure<KotlinterExtension> {
    indentSize = 2
  }
}

task<Delete>("clean") {
  delete(rootProject.buildDir)
}
