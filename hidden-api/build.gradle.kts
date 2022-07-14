plugins {
  id(libs.plugins.android.library.get().pluginId)
  id(libs.plugins.kotlin.android.get().pluginId)
}

setupLibraryModule() {
  defaultConfig {
    namespace = "com.absinthe.libchecker.hidden_api"
  }
}

dependencies {
  annotationProcessor(libs.rikka.refine.compiler)
  compileOnly(libs.rikka.refine.annotation)

  implementation(libs.androidX.annotation)
}
