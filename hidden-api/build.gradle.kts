@Suppress(
  "DSL_SCOPE_VIOLATION",
  "MISSING_DEPENDENCY_CLASS",
  "UNRESOLVED_REFERENCE_WRONG_RECEIVER",
  "FUNCTION_CALL_EXPECTED"
)

plugins {
  alias(libs.plugins.android.library)
}

setupLibraryModule {
  defaultConfig {
    namespace = "com.absinthe.libchecker.hidden_api"
  }
}

dependencies {
  annotationProcessor(libs.rikka.refine.compiler)
  compileOnly(libs.rikka.refine.annotation)
}
