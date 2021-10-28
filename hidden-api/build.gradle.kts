plugins {
  id("com.android.library")
  kotlin("android")
}

setupLibraryModule()

dependencies {
  annotationProcessor(Libs.refineAnnotationCompiler)
  compileOnly(Libs.refineAnnotation)

  implementation(Libs.annotation)
}
