// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.2.0-rc01")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.32")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
        classpath("com.tencent.mm:AndResGuard-gradle-plugin:1.2.21") // Resource obfuscate
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.15")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://dl.bintray.com/absinthe/libraries")
    }
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
