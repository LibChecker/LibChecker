plugins {
  id(libs.plugins.android.library.get().pluginId)
  id(libs.plugins.kotlin.android.get().pluginId)
}

setupLibraryModule()

dependencies {
  annotationProcessor(Libs.refineAnnotationCompiler)
  compileOnly(Libs.refineAnnotation)

  implementation(Libs.annotation)
}
