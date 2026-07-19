plugins {
  alias(libs.plugins.android.library)
  id("build-logic")
}

setupLibraryModule {
  namespace = "com.absinthe.libchecker.hidden_api"
  enableKotlin = false
}

dependencies {
  annotationProcessor(libs.rikka.refine.compiler)
  compileOnly(libs.rikka.refine.annotation)
  compileOnly(libs.androidX.annotation)
}
