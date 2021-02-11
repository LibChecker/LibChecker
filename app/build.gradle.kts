import com.google.protobuf.gradle.*
import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
    id("com.google.protobuf")
}
apply {
    from("and_res_guard.gradle")
}

android {
    compileSdkVersion(30)
    buildToolsVersion = "30.0.3"

    val gitCommitId = "git rev-parse --short HEAD".runCommand(project.rootDir)
    val gitCommitCount = "git rev-list --count HEAD".runCommand(project.rootDir).toInt()
    val baseVersionName = "2.0.0-beta05"

    defaultConfig {
        applicationId = "com.absinthe.libchecker"
        minSdkVersion(23)
        targetSdkVersion(30)
        versionCode = gitCommitCount
        versionName = "${baseVersionName}.${gitCommitId}"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resConfigs("en", "zh", "zh-rHK", "zh-rTW")
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            isZipAlignEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // To inline the bytecode built with JVM target 1.8 into
    // bytecode that is being built with JVM target 1.6. (e.g. navArgs)

    sourceSets {
        getByName("main").java.apply {
            srcDirs("src/main/kotlin")
            srcDirs("src/main/java")
            srcDirs("src/main/proto")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-XXLanguage:+InlineClasses")
    }

    packagingOptions {
        exclude("META-INF/atomicfu.kotlin_module")
    }

    applicationVariants.all { variant ->
        variant.outputs
            .map { it as BaseVariantOutputImpl }
            .forEach { output ->
                output.outputFileName = "LibChecker-${variant.versionName}.apk"
            }
        true
    }
}

configurations.all {
    exclude(group = "rikka.appcompat", module = "appcompat")
}

val grpcVersion by extra("1.35.0")
val protocVersion by extra("3.14.0")

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.2")

    implementation("com.absinthe.libraries.me:me:1.0.6")
    implementation("com.absinthe.libraries.utils:utils:1.1.7")

    val appCenterSdkVersion = "4.1.0"
    implementation("com.microsoft.appcenter:appcenter-analytics:${appCenterSdkVersion}")
    implementation("com.microsoft.appcenter:appcenter-crashes:${appCenterSdkVersion}")

    implementation("androidx.fragment:fragment-ktx:1.3.0")
    implementation("androidx.appcompat:appcompat:1.2.0")

    // Lifecycle
    val lifecycleVersion = "2.3.0"
    implementation("androidx.lifecycle:lifecycle-common-java8:${lifecycleVersion}")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:${lifecycleVersion}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${lifecycleVersion}")
    implementation("androidx.lifecycle:lifecycle-process:$lifecycleVersion")

    // Room components
    val roomVersion = "2.2.6"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    //implementation("org.xerial:sqlite-jdbc:3.34.0")

    implementation("androidx.core:core-ktx:1.5.0-beta01")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.viewpager2:viewpager2:1.1.0-alpha01")
    implementation("androidx.recyclerview:recyclerview:1.2.0-beta01")
    implementation("androidx.browser:browser:1.3.0")

    implementation("com.google.android.material:material:1.3.0")
    implementation("com.github.CymChad:BaseRecyclerViewAdapterHelper:3.0.6")
    implementation("com.drakeet.about:about:2.4.1")
    implementation("com.drakeet.multitype:multitype:4.2.0")
    implementation("com.airbnb.android:lottie:3.6.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.jonathanfinerty.once:once:1.3.0")
    implementation("net.dongliu:apk-parser:2.6.10")
    implementation("io.coil-kt:coil:1.1.1")
    implementation("me.zhanghai.android.fastscroll:library:1.1.5")
    implementation("me.zhanghai.android.appiconloader:appiconloader:1.3.0")

    //Serilization
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("com.google.protobuf:protobuf-javalite:$protocVersion")

//    implementation("rikka.appcompat:appcompat:1.2.0-rc01")
    implementation("rikka.core:core:1.2.3")
    implementation("rikka.material:material:1.4.3")
    implementation("rikka.recyclerview:recyclerview-utils:1.1.0")

    val rikkaPreference = "4.2.0-alpha03"
    implementation("moe.shizuku.preference:preference-appcompat:$rikkaPreference")
    implementation("moe.shizuku.preference:preference-simplemenu-appcompat:$rikkaPreference")

    //XML layout to Java code
    implementation("com.zhangyue.we:x2c-lib:1.0.6")
    annotationProcessor("com.zhangyue.we:x2c-apt:1.1.2")

    //Network
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okio:okio:2.10.0")

    // gRPC
    implementation("io.grpc:grpc-okhttp:$grpcVersion")
    implementation("io.grpc:grpc-protobuf-lite:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    testImplementation("junit:junit:4.13.1")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.6")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protocVersion"
    }
    plugins {
        // Optional: an artifact spec for a protoc plugin, with "grpc" as
        // the identifier, which can be referred to in the "plugins"
        // container of the "generateProtoTasks" closure.
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        generateProtoTasks {
            all().forEach {
                it.builtins {
                    create("java") {
                        option("lite")
                    }
                }

                it.plugins{
                    create("grpc") {
                        option("lite")
                    }
                }
            }
        }
    }

}

fun String.runCommand(workingDir: File = file("./")): String {
    val parts = this.split("\\s".toRegex())
    val proc = ProcessBuilder(*parts.toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    proc.waitFor(1, TimeUnit.MINUTES)
    return proc.inputStream.bufferedReader().readText().trim()
}
