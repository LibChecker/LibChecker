plugins {
  `kotlin-dsl`
}

apply("../gradle/extra.gradle.kts")

dependencies {
  implementation(rootProject.extra["androidPlugin"].toString())
  implementation(rootProject.extra["kotlinPlugin"].toString())
}
