plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
}

android {
    compileSdk = 31
    buildToolsVersion = "31.0.0"

    defaultConfig {
        minSdk = 23
        targetSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-XXLanguage:+InlineClasses")
    }

    compileOptions {
        targetCompatibility(JavaVersion.VERSION_11)
        sourceCompatibility(JavaVersion.VERSION_11)
    }

    packagingOptions.resources.excludes += setOf(
        "META-INF/atomicfu.kotlin_module"
    )
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.1-native-mt")

    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.core:core-ktx:1.6.0")

    testImplementation("junit:junit:4.13.2")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.7")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
