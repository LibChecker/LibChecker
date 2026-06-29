plugins {
  alias(libs.plugins.android.test)
}

android {
  namespace = "com.absinthe.libchecker.macrobenchmark"
  compileSdk = 37

  defaultConfig {
    minSdk = 24
    targetSdk = 37
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  targetProjectPath = ":app"
  experimentalProperties["android.experimental.self-instrumenting"] = true

  buildTypes {
    create("benchmark") {
      isMinifyEnabled = true
      matchingFallbacks += listOf("release")
      testProguardFiles("proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debug")
    }
  }

  flavorDimensions += "channel"
  productFlavors {
    create("foss") {
      dimension = "channel"
    }
    create("market") {
      dimension = "channel"
    }
  }
}

dependencies {
  implementation(libs.androidX.benchmark.macro.junit4)
  implementation(libs.androidX.collection)
  implementation(libs.androidX.test.ext.junit)
  implementation(libs.androidX.test.runner)
  implementation(libs.androidX.test.uiautomator)
}
