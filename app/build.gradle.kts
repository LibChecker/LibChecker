import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.google.protobuf.gradle.builtins
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.nio.file.Paths

plugins {
  id(libs.plugins.android.application.get().pluginId)
  id(libs.plugins.kotlin.android.get().pluginId)
  id(libs.plugins.kotlin.parcelize.get().pluginId)
  alias(libs.plugins.protobuf)
  alias(libs.plugins.hiddenApiRefine)
  alias(libs.plugins.ksp)
}

ksp {
  arg("moshi.generated", "javax.annotation.Generated")
  arg("room.incremental", "true")
  arg("room.schemaLocation", "$projectDir/schemas")
}

setupAppModule {
  defaultConfig {
    applicationId = "com.absinthe.libchecker"
    namespace = "com.absinthe.libchecker"
  }

  buildFeatures {
    viewBinding = true
  }

  compileOptions {
    isCoreLibraryDesugaringEnabled = true
  }

  sourceSets {
    named("main") {
      java {
        srcDirs("src/main/kotlin")
      }
    }
    named("foss") {
      java {
        srcDirs("src/foss/kotlin")
      }
    }
    named("market") {
      java {
        srcDirs("src/market/kotlin")
      }
    }
  }

  packagingOptions.resources.excludes += setOf(
    "META-INF/**",
    "okhttp3/**",
    "kotlin/**",
    "org/**",
    "**.properties",
    "**.bin"
  )

  lint {
    disable += "AppCompatResource"
  }

  dependenciesInfo.includeInApk = false

  applicationVariants.all {
    outputs.all {
      (this as? ApkVariantOutputImpl)?.outputFileName =
        "LibChecker-${verName}-${verCode}-${name}.apk"
    }
  }
}

tasks.whenTaskAdded {
  if (name == "optimizeFossReleaseResources" || name == "optimizeMarketReleaseResources") {
    val flavor = name.removePrefix("optimize").removeSuffix("ReleaseResources")
    val flavorLowerCase = flavor.toLowerCaseAsciiOnly()
    val optimizeReleaseRes = task("optimize${flavor}ReleaseRes").doLast {
      val aapt2 = File(
        androidComponents.sdkComponents.sdkDirectory.get().asFile,
        "build-tools/${project.android.buildToolsVersion}/aapt2"
      )
      val zip = Paths.get(
        buildDir.path,
        "intermediates",
        "optimized_processed_res",
        "${flavorLowerCase}Release",
        "resources-${flavorLowerCase}-release-optimize.ap_"
      )
      val optimized = File("${zip}.opt")
      val cmd = exec {
        commandLine(
          aapt2, "optimize",
          "--collapse-resource-names",
          "--resources-config-path", "aapt2-resources.cfg",
          "-o", optimized,
          zip
        )
        isIgnoreExitValue = false
      }
      if (cmd.exitValue == 0) {
        delete(zip)
        optimized.renameTo(zip.toFile())
      }
    }

    finalizedBy(optimizeReleaseRes)
  }
}

configurations.all {
  exclude("androidx.appcompat", "appcompat")
  exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
  exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
}

dependencies {
  compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
  compileOnly(fileTree("ohos"))
  compileOnly(projects.hiddenApi)

  coreLibraryDesugaring(libs.agp.desugering)

  implementation(libs.kotlinX.coroutines)
  implementation(libs.androidX.appCompat)
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
  implementation(libs.bundles.androidX.room)
  implementation(libs.google.material)
  implementation(libs.coil)
  implementation(libs.square.okHttp)
  implementation(libs.square.okio)
  implementation(libs.square.retrofit)
  implementation(libs.square.retrofit.moshi)
  implementation(libs.square.moshi)
  implementation(libs.google.protobuf.javaLite)
  implementation(libs.bundles.grpc)
  implementation(libs.rikka.refine.runtime)
  implementation(libs.bundles.zhaobozhen)
  implementation(libs.bundles.appCenter)
  implementation(libs.lc.rules)

  ksp(libs.androidX.room.compiler)
  ksp(libs.square.moshi.compiler)

  implementation(libs.lottie)
  implementation(libs.drakeet.about)
  implementation(libs.drakeet.multitype)
  implementation(libs.brvah)
  implementation(libs.mpAndroidChart)
  implementation(libs.timber)
  implementation(libs.processPhoenix)
  implementation(libs.once)
  implementation(libs.cascade)
  implementation(libs.fastScroll)
  implementation(libs.appIconLoader)
  implementation(libs.hiddenApiBypass)
  implementation(libs.dexLib2)
  implementation(libs.slf4j)

  implementation(libs.bundles.rikkax)

  debugImplementation(libs.square.leakCanary)
}

protobuf {
  protoc {
    artifact = if (osdetector.os == "osx")
      "${libs.google.protobuf.protoc.get()}:osx-aarch_64"
    else
      libs.google.protobuf.protoc.get().toString()
  }
  plugins {
    // Optional: an artifact spec for a protoc plugin, with "grpc" as
    // the identifier, which can be referred to in the "plugins"
    // container of the "generateProtoTasks" closure.
    id("grpc") {
      artifact = if (osdetector.os == "osx")
        "${libs.grpc.gen.get()}:osx-aarch_64"
      else
        libs.grpc.gen.get().toString()
    }
    generateProtoTasks {
      all().forEach {
        it.builtins {
          create("java") {
            option("lite")
          }
        }

        it.plugins {
          create("grpc") {
            option("lite")
          }
        }
      }
    }
  }
}
