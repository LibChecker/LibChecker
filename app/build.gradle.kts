import com.google.protobuf.gradle.*
import java.nio.charset.Charset
import java.nio.file.Paths
import com.android.build.api.variant.impl.ApplicationVariantImpl
import com.android.build.gradle.internal.dsl.BuildType

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
    id("com.google.protobuf")
}

apply("and_res_guard.gradle")

android {
    compileSdk = 30
    buildToolsVersion = "30.0.3"

    val gitCommitId = "git rev-parse --short HEAD".exec()
    val baseVersionName = "2.0.14"
    val verName = "${baseVersionName}.${gitCommitId}"
    val verCode = "git rev-list --count HEAD".exec().toInt()

    defaultConfig {
        applicationId = "com.absinthe.libchecker"
        minSdk = 23
        targetSdk = 30
        versionCode = verCode
        versionName = verName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += arrayOf("en", "zh-rCN", "zh-rTW", "ru", "uk-rUA")
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

    compileOptions {
        targetCompatibility(JavaVersion.VERSION_11)
        sourceCompatibility(JavaVersion.VERSION_11)
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            (this as BuildType).isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    kotlinOptions {
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-XXLanguage:+InlineClasses")
    }

    packagingOptions.resources.excludes += setOf(
        "META-INF/**",
        "okhttp3/**",
        "kotlin/**",
        "org/**",
        "**.properties",
        "**.bin"
    )

    dependenciesInfo.includeInApk = false

    androidComponents.onVariants { v ->
        val variant = v as ApplicationVariantImpl
        variant.outputs.forEach {
            it.outputFileName.set("LibChecker-${verName}-${verCode}-${variant.name}.apk")
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
        "shrunk_processed_res",
        "release",
        "resources-release-stripped.ap_"
    )
    val optimized = File("${zip}.opt")
    val cmd = exec {
        commandLine(
            aapt2, "optimize",
            "--collapse-resource-names",
            "--shorten-resource-paths",
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
    if (name == "shrinkReleaseRes") {
        finalizedBy(optimizeReleaseRes)
    }
}

configurations.all {
    exclude(group = "androidx.appcompat", module = "appcompat")
}

val grpcVersion by extra("1.38.1")
val protocVersion by extra("3.17.0")

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.0")

    implementation("com.github.zhaobozhen.libraries:me:1.0.1")
    implementation("com.github.zhaobozhen.libraries:utils:1.0.1")

    val appCenterSdkVersion = "4.2.0"
    implementation("com.microsoft.appcenter:appcenter-analytics:${appCenterSdkVersion}")
    implementation("com.microsoft.appcenter:appcenter-crashes:${appCenterSdkVersion}")

    implementation("androidx.fragment:fragment-ktx:1.3.5")
    implementation("androidx.activity:activity-ktx:1.2.3")
    // Lifecycle
    val lifecycleVersion = "2.3.1"
    implementation("androidx.lifecycle:lifecycle-common-java8:${lifecycleVersion}")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:${lifecycleVersion}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${lifecycleVersion}")
    implementation("androidx.lifecycle:lifecycle-process:$lifecycleVersion")

    // Room components
    val roomVersion = "2.3.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("org.xerial:sqlite-jdbc:3.34.0") //Work around on Apple Silicon
    kapt("androidx.room:room-compiler:$roomVersion")

    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.browser:browser:1.3.0")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.viewpager2:viewpager2:1.1.0-alpha01")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.core:core-ktx:1.6.0-rc01")

    implementation("com.google.android.material:material:1.4.0-rc01")
    implementation("com.github.CymChad:BaseRecyclerViewAdapterHelper:3.0.6")
    implementation("com.github.PhilJay:MPAndroidChart:3.1.0")
    implementation("com.drakeet.about:about:2.4.1")
    implementation("com.drakeet.multitype:multitype:4.3.0")
    implementation("com.airbnb.android:lottie:3.7.0")
    implementation("com.jakewharton.timber:timber:4.7.1")
    implementation("com.jonathanfinerty.once:once:1.3.1")
    implementation("net.dongliu:apk-parser:2.6.10")
    implementation("io.coil-kt:coil:1.2.2")
    implementation("me.zhanghai.android.fastscroll:library:1.1.7")
    implementation("me.zhanghai.android.appiconloader:appiconloader:1.3.1")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:2.0")

    //Serialization
    implementation("com.google.code.gson:gson:2.8.7")
    implementation("com.google.protobuf:protobuf-javalite:$protocVersion")

    implementation("dev.rikka.rikkax.appcompat:appcompat:1.2.0-rc01")
    implementation("dev.rikka.rikkax.core:core:1.3.2")
    implementation("dev.rikka.rikkax.material:material:1.6.5")
    implementation("dev.rikka.rikkax.recyclerview:recyclerview-ktx:1.2.2")
    implementation("dev.rikka.rikkax.widget:borderview:1.0.1")
    implementation("dev.rikka.rikkax.preference:simplemenu-preference:1.0.3")
    implementation("dev.rikka.rikkax.insets:insets:1.1.0")

    //Network
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okio:okio:2.10.0")

    // gRPC
    implementation("io.grpc:grpc-okhttp:$grpcVersion")
    implementation("io.grpc:grpc-protobuf-lite:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    testImplementation("junit:junit:4.13.2")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.7")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}

protobuf {
    protoc {
        artifact = if (osdetector.os == "osx") {
            "com.google.protobuf:protoc:$protocVersion:osx-x86_64"
        } else {
            "com.google.protobuf:protoc:$protocVersion"
        }
    }
    plugins {
        // Optional: an artifact spec for a protoc plugin, with "grpc" as
        // the identifier, which can be referred to in the "plugins"
        // container of the "generateProtoTasks" closure.
        id("grpc") {
            artifact = if (osdetector.os == "osx") {
                "io.grpc:protoc-gen-grpc-java:$grpcVersion:osx-x86_64"
            } else {
                "io.grpc:protoc-gen-grpc-java:$grpcVersion"
            }
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

fun String.exec(): String = Runtime.getRuntime().exec(this).inputStream.readBytes()
    .toString(Charset.defaultCharset()).trim()

