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
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.1-native-mt")

  implementation("androidx.appcompat:appcompat:1.3.1")
  implementation("androidx.core:core-ktx:1.6.0")

  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.1.3")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
