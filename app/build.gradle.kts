import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.google.protobuf.gradle.builtins
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import java.nio.file.Paths

plugins {
  id("com.android.application")
  kotlin("android")
  kotlin("kapt")
  kotlin("plugin.parcelize")
  id("com.google.protobuf")
  id("dev.rikka.tools.refine.gradle-plugin")
}

setupAppModule {
  defaultConfig {
    applicationId = "com.absinthe.libchecker"
  }

  buildFeatures {
    viewBinding = true
  }

  kapt {
    arguments {
      arg("room.incremental", "true")
      arg("room.schemaLocation", "$projectDir/schemas")
    }
  }

  sourceSets["main"].java.srcDirs("src/main/kotlin")

  packagingOptions.resources.excludes += setOf(
    "META-INF/**",
    "okhttp3/**",
    "kotlin/**",
    "org/**",
    "**.properties",
    "**.bin"
  )

  dependenciesInfo.includeInApk = false

  applicationVariants.all {
    outputs.all {
      (this as? ApkVariantOutputImpl)?.outputFileName =
        "LibChecker-${verName}-${verCode}-${name}.apk"
    }
  }
}

val optimizeReleaseRes = task("optimizeReleaseRes").doLast {
  val aapt2 = File(
    androidComponents.sdkComponents.sdkDirectory.get().asFile,
    "build-tools/${project.android.buildToolsVersion}/aapt2"
  )
  val zip = Paths.get(
    project.buildDir.path,
    "intermediates",
    "optimized_processed_res",
    "release",
    "resources-release-optimize.ap_"
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

tasks.whenTaskAdded {
  if (name == "optimizeReleaseResources") {
    finalizedBy(optimizeReleaseRes)
  }
}

configurations.all {
  exclude(group = "androidx.appcompat", module = "appcompat")
}

dependencies {
  compileOnly(fileTree("ohos"))
  compileOnly(project(":hidden-api"))

  implementations(
    // UI
    Libs.activity,
    Libs.fragment,
    *Libs.lifecycle,
    *Libs.room,
    Libs.constraintLayout,
    Libs.browser,
    Libs.viewPager2,
    Libs.recyclerView,
    Libs.preference,
    Libs.material,

    // Net
    Libs.coil,
    Libs.okHttp,
    Libs.retrofit,

    // Serialization
    *Libs.moshi,
    Libs.protobufJavaLite,

    // gRPC
    *Libs.grpc,

    // Debug
    *Libs.appCenter,

    Libs.refineRuntime
  )

  implementation("com.github.zhaobozhen.libraries:me:1.0.2")
  implementation("com.github.zhaobozhen.libraries:utils:1.0.2")

  kapt(Libs.roomCompiler)
  kapt(Libs.moshiCompiler)

  implementation("com.github.CymChad:BaseRecyclerViewAdapterHelper:3.0.7")
  implementation("com.github.PhilJay:MPAndroidChart:3.1.0")
  implementation("com.drakeet.about:about:2.5.0")
  implementation("com.drakeet.multitype:multitype:4.3.0")
  implementation("com.airbnb.android:lottie:4.2.2")
  implementation("com.jakewharton.timber:timber:5.0.1")
  implementation("com.jakewharton:process-phoenix:2.1.2")
  implementation("com.jonathanfinerty.once:once:1.3.1")
  implementation("net.dongliu:apk-parser:2.6.10")
  implementation("me.zhanghai.android.fastscroll:library:1.1.7")
  implementation("me.zhanghai.android.appiconloader:appiconloader:1.3.1")
  implementation("org.lsposed.hiddenapibypass:hiddenapibypass:2.0")

  implementation("dev.rikka.rikkax.appcompat:appcompat:1.2.0-rc01")
  implementation("dev.rikka.rikkax.core:core:1.3.3")
  implementation("dev.rikka.rikkax.material:material:1.6.6")
  implementation("dev.rikka.rikkax.recyclerview:recyclerview-ktx:1.2.2")
  implementation("dev.rikka.rikkax.widget:borderview:1.1.0")
  implementation("dev.rikka.rikkax.preference:simplemenu-preference:1.0.3")
  implementation("dev.rikka.rikkax.insets:insets:1.1.0")

  debugImplementation(Libs.leakCanary)
}

protobuf {
  protoc {
    artifact = if (osdetector.os == "osx") Libs.protocMac else Libs.protoc
  }
  plugins {
    // Optional: an artifact spec for a protoc plugin, with "grpc" as
    // the identifier, which can be referred to in the "plugins"
    // container of the "generateProtoTasks" closure.
    id("grpc") {
      artifact = if (osdetector.os == "osx") Libs.genGrpcMac else Libs.genGrpc
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
