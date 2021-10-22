rootProject.extra.run {
  set("androidPlugin", "com.android.tools.build:gradle:7.0.3")
  set("kotlinPlugin", "org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31")
}

repositories {
  google()
  mavenCentral()
  maven("https://jitpack.io")
}
