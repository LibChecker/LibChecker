rootProject.extra.run {
  set("androidPlugin", "com.android.tools.build:gradle:7.0.4")
  set("kotlinPlugin", "org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.0")
}

repositories {
  google()
  mavenCentral()
  maven("https://jitpack.io")
}
