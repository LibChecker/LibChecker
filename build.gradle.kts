plugins {
  id(libs.plugins.android.application.get().pluginId) apply false
  id(libs.plugins.android.library.get().pluginId) apply false
  id(libs.plugins.kotlin.android.get().pluginId) apply false
  alias(libs.plugins.protobuf) apply false
  alias(libs.plugins.kotlinter) apply false
  alias(libs.plugins.hiddenApiRefine) apply false
  alias(libs.plugins.ksp) apply false
}

allprojects {
  apply(plugin = rootProject.libs.plugins.kotlinter.get().pluginId)

  tasks.matching {
    it.name.contains("transformClassesWithHiddenApiRefine")
  }.configureEach {
    notCompatibleWithConfigurationCache("transformClassesWithHiddenApiRefine task can't support CC")
  }
}

task<Delete>("clean") {
  delete(rootProject.buildDir)
}
