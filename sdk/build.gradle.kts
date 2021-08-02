plugins {
  id("com.android.library")
  kotlin("android")
  kotlin("kapt")
}

setupLibraryModule {
  packagingOptions.resources.excludes += setOf(
    "META-INF/atomicfu.kotlin_module"
  )
}

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.1-native-mt")
}
