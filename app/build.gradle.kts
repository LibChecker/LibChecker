import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.parcelize)
  alias(libs.plugins.protobuf)
  alias(libs.plugins.hiddenApiRefine)
  alias(libs.plugins.ksp)
  alias(libs.plugins.androidX.room3)
  alias(libs.plugins.moshiX)
  alias(libs.plugins.aboutlibraries)
  alias(libs.plugins.gms)
  alias(libs.plugins.firebase.crashlytics)
  id("build-logic")
  id("res-opt")
  id("market-stable-manifest")
}

ksp {
  arg("moshi.generated", "javax.annotation.Generated")
}

room3 {
  schemaDirectory("$projectDir/schemas")
}

setupAppModule {
  namespace = "com.absinthe.libchecker"
  defaultConfig {
    applicationId = "com.absinthe.libchecker"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    release {
      optimization {
        enable = true
        keepRules {
          // https://github.com/AppDevNext/AndroidChart/blob/master/chartLib/proguard-lib.pro
          ignoreFrom(libs.mpAndroidChart.get().module.toString())
        }
      }
    }
    create("benchmark") {
      initWith(getByName("release"))
      applicationIdSuffix = ".debug"
      matchingFallbacks += listOf("release")
      proguardFiles("src/benchmark/keepRules/proguard-rules.keep")
      signingConfig = signingConfigs.getByName("debug")
    }
  }

  productFlavors {
    flavorDimensions += "channel"

    create("foss") {
      isDefault = true
      dimension = flavorDimensions[0]
      buildConfigField("Boolean", "IS_FOSS", "true")
      configure<CrashlyticsExtension> {
        mappingFileUploadEnabled = false
      }
    }
    create("market") {
      dimension = flavorDimensions[0]
      buildConfigField("Boolean", "IS_FOSS", "false")
    }
    configureEach {
      manifestPlaceholders["channel"] = this.name
    }
  }

  packaging {
    jniLibs {
      excludes += "lib/**/libdatastore_shared_counter.so" // Jetpack DataStore
    }
    resources {
      excludes += setOf(
        "META-INF/**",
        "okhttp3/**",
        "kotlin/**",
        "org/**",
        "**.properties",
        "**.bin",
        "**/*.proto"
      )
    }
  }

  lint {
    disable += setOf("AppCompatResource", "MissingTranslation")
  }

  dependenciesInfo.includeInApk = false
}

androidComponents {
  onVariants { variant ->
    variant.outputs.forEach { output ->
      output.outputFileName.set(
        output.versionName.zip(output.versionCode) { versionName, versionCode ->
          "LibChecker-$versionName-$versionCode-${variant.buildType}.apk"
        }
      )
    }
  }
}

dependencies {
  compileOnly(dependencies.project(":hidden-api"))

  implementation(projects.compat)
  implementation(libs.kotlinX.coroutines)
  implementation(platform(libs.koin.bom))
  implementation(libs.koin.android)
  implementation(libs.androidX.core)
  implementation(libs.androidX.activity)
  implementation(libs.androidX.fragment)
  implementation(libs.androidX.constraintLayout)
  implementation(libs.androidX.browser)
  implementation(libs.androidX.viewPager2)
  implementation(libs.androidX.recyclerView)
  implementation(libs.androidX.preference)
  implementation(libs.androidX.window)
  implementation(libs.bundles.androidX.lifecycle)
  implementation(libs.bundles.androidX.room3)
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
  ksp(libs.androidX.room3.compiler)

  testImplementation(libs.junit)

  androidTestImplementation(libs.androidX.test.ext.junit)
  androidTestImplementation(libs.androidX.test.runner)

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
      all().configureEach {
        builtins {
          create("java") {
            option("lite")
          }
        }
      }
    }
  }
}
