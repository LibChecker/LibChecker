import org.jmailen.gradle.kotlinter.KotlinterExtension

buildscript {
  repositories {
    google()
    gradlePluginPortal()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:7.0.0-rc01")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.21")
    classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.17")
    classpath("org.jmailen.gradle:kotlinter-gradle:3.4.5")
  }
}

allprojects {
  apply(plugin = "org.jmailen.kotlinter")

  configure<KotlinterExtension> {
    indentSize = 2
  }

  repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
  }
}

task<Delete>("clean") {
  delete(rootProject.buildDir)
}
