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
    classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.17")
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
