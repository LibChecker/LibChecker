plugins {
  alias(libs.plugins.android.library)
  id("build-logic")
}

setupLibraryModule {
  namespace = "com.absinthe.libchecker.compat"

  defaultConfig {
    minSdk = 24
  }

  flavorDimensions += "channel"
  productFlavors {
    create("foss") {
      dimension = flavorDimensions[0]
    }
    create("market") {
      dimension = flavorDimensions[0]
    }
  }
}

dependencies {
  api(libs.androidX.annotation)
  api(libs.androidX.recyclerView)
  api(libs.brvah)
  api(libs.google.material)

  implementation(libs.timber)

  "marketCompileOnly"(fileTree("../app/ohos"))
}
