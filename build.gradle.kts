@Suppress(
  "DSL_SCOPE_VIOLATION",
  "MISSING_DEPENDENCY_CLASS",
  "UNRESOLVED_REFERENCE_WRONG_RECEIVER",
  "FUNCTION_CALL_EXPECTED"
)

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.parcelize) apply false
  alias(libs.plugins.protobuf) apply false
  alias(libs.plugins.kotlinter) apply false
  alias(libs.plugins.hiddenApiRefine) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.moshiX) apply false
}

allprojects {
  apply(plugin = rootProject.libs.plugins.kotlinter.get().pluginId)

  tasks.matching {
    it.name.contains("transformClassesWithHiddenApiRefine")
  }.configureEach {
    notCompatibleWithConfigurationCache("https://github.com/RikkaApps/HiddenApiRefinePlugin/issues/9")
  }
  tasks.matching {
    it.name.contains("optimize(.*)ReleaseRes".toRegex())
  }.configureEach {
    notCompatibleWithConfigurationCache("optimizeReleaseRes tasks haven't support CC.")
  }
}

task<Delete>("clean") {
  delete(rootProject.buildDir)
}
