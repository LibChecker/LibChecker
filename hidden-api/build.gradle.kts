plugins {
  alias(libs.plugins.android.library)
}

setupLibraryModule {
  namespace = "com.absinthe.libchecker.hidden_api"
}

dependencies {
  annotationProcessor(libs.rikka.refine.compiler)
  compileOnly(libs.rikka.refine.annotation)
  implementation(libs.androidX.annotation)
}
