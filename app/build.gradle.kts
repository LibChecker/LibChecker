import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.parcelize)
  alias(libs.plugins.protobuf)
  alias(libs.plugins.hiddenApiRefine)
  alias(libs.plugins.ksp)
  alias(libs.plugins.moshiX)
  alias(libs.plugins.aboutlibraries)
  id("res-opt") apply false
  id(libs.plugins.gms.get().pluginId)
  id(libs.plugins.firebase.crashlytics.get().pluginId)
}

ksp {
  arg("moshi.generated", "javax.annotation.Generated")
  arg("room.generateKotlin", "true")
  arg("room.incremental", "true")
  arg("room.schemaLocation", "$projectDir/schemas")
  arg("room.expandProjection", "true")
}

setupAppModule {
  namespace = "com.absinthe.libchecker"
  defaultConfig {
    applicationId = "com.absinthe.libchecker"
  }
  androidResources {
    generateLocaleConfig = true
  }

  buildFeatures {
    aidl = true
    buildConfig = true
    viewBinding = true
  }

  buildTypes {
    debug {
      configure<CrashlyticsExtension> {
        mappingFileUploadEnabled = false
      }
    }
  }

  productFlavors {
    flavorDimensions += "channel"

    create("foss") {
      isDefault = true
      dimension = flavorDimensionList[0]
      configure<CrashlyticsExtension> {
        mappingFileUploadEnabled = false
      }
    }
    create("market") {
      dimension = flavorDimensionList[0]
    }
    all {
      manifestPlaceholders["channel"] = this.name
    }
  }

  packagingOptions.resources.excludes += setOf(
    "META-INF/**",
    "okhttp3/**",
    "kotlin/**",
    "org/**",
    "**.properties",
    "**.bin",
    "**/*.proto"
  )

  lint {
    disable += "AppCompatResource"
  }

  dependenciesInfo.includeInApk = false

  applicationVariants.configureEach {
    outputs.configureEach {
      (this as? ApkVariantOutputImpl)?.outputFileName =
        "LibChecker-$verName-$verCode-$name.apk"
    }
  }
}

configurations.configureEach {
  exclude("androidx.appcompat", "appcompat")
  exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
  exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
}

dependencies {
  compileOnly(projects.hiddenApi)

  implementation(libs.kotlinX.coroutines)
  // implementation(libs.androidX.appCompat)
  implementation(libs.androidX.core)
  implementation(libs.androidX.activity)
  implementation(libs.androidX.fragment)
  implementation(libs.androidX.constraintLayout)
  implementation(libs.androidX.browser)
  implementation(libs.androidX.viewPager2)
  implementation(libs.androidX.recyclerView)
  implementation(libs.androidX.preference)
  implementation(libs.androidX.window)
  implementation(libs.androidX.security)
  implementation(libs.bundles.androidX.lifecycle)
  implementation(libs.bundles.androidX.room)
  implementation(libs.google.material)
  implementation(libs.coil)
  implementation(libs.square.okHttp)
  implementation(libs.square.okio)
  implementation(libs.square.retrofit)
  implementation(libs.square.retrofit.moshi)
  implementation(libs.square.moshi)
  implementation(libs.google.protobuf.javaLite)
  implementation(libs.google.dexlib2)
  implementation(libs.rikka.refine.runtime)
  implementation(libs.bundles.zhaobozhen)
  implementation(libs.lc.rules)

  ksp(libs.androidX.room.compiler)

  implementation(libs.lottie)
  implementation(libs.aboutlibraries.core)
  implementation(libs.aboutlibraries.ui)
  implementation(libs.brvah)
  implementation(libs.mpAndroidChart)
  implementation(libs.timber)
  implementation(libs.processPhoenix)
  implementation(libs.once)
  implementation(libs.fastScroll)
  implementation(libs.appIconLoader)
  implementation(libs.appIconLoader.coil)
  implementation(libs.hiddenApiBypass)
  implementation(libs.commons.compress)
  implementation(libs.flexbox)

  implementation(libs.bundles.rikkax)

  implementation(libs.bundles.shizuku)

  debugImplementation(libs.square.leakCanary)
  "marketCompileOnly"(fileTree("ohos"))
  "marketImplementation"(platform(libs.firebase.bom))
  "marketImplementation"(libs.bundles.firebase) {
    exclude(group = "com.google.android.gms", module = "play-services-ads-identifier")
  }
}

protobuf {
  protoc {
    artifact = if (osdetector.os == "osx") {
      // support both Apple Silicon and Intel chipsets
      val arch = System.getProperty("os.arch")
      val suffix = if (arch == "x86_64") "x86_64" else "aarch_64"
      "${libs.google.protobuf.protoc.get()}:osx-$suffix"
    } else {
      libs.google.protobuf.protoc.get().toString()
    }
  }
  plugins {
    generateProtoTasks {
      all().forEach {
        it.builtins {
          create("java") {
            option("lite")
          }
        }
      }
    }
  }
}
