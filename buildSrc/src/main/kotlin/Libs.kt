@file:Suppress("SpellCheckingInspection")

private const val lifecycleVersion = "2.4.1"
private const val retrofitVersion = "2.9.0"
private const val roomVersion = "2.4.1"
private const val moshiVersion = "1.13.0"
private const val grpcVersion = "1.44.1"
private const val protocVersion = "3.19.4"
private const val appCenterVersion = "4.4.2"
private const val hiddenApiRefineVersion = "3.0.3"
private const val myLib = "1.0.4"

object Libs {
  const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0"
  const val core = "androidx.core:core-ktx:1.7.0"
  const val appCompat = "androidx.appcompat:appcompat:1.4.1"
  const val annotation = "androidx.annotation:annotation:1.3.0"
  const val activity = "androidx.activity:activity-ktx:1.4.0"
  const val fragment = "androidx.fragment:fragment-ktx:1.4.1"
  const val constraintLayout = "androidx.constraintlayout:constraintlayout:2.1.3"
  const val browser = "androidx.browser:browser:1.4.0"
  const val recyclerView = "androidx.recyclerview:recyclerview:1.2.1"
  const val preference = "androidx.preference:preference-ktx:1.2.0"
  const val window = "androidx.window:window:1.0.0"
  const val viewPager2 = "androidx.viewpager2:viewpager2:1.1.0-alpha01"
  const val material = "com.google.android.material:material:1.5.0"

  const val leakCanary = "com.squareup.leakcanary:leakcanary-android:2.8.1"
  const val okHttp = "com.squareup.okhttp3:okhttp:4.9.3"
  const val coil = "io.coil-kt:coil:1.4.0"
  const val retrofit = "com.squareup.retrofit2:retrofit:$retrofitVersion"
  const val protobufJavaLite = "com.google.protobuf:protobuf-javalite:$protocVersion"
  const val protoc = "com.google.protobuf:protoc:$protocVersion"
  const val protocMac = "com.google.protobuf:protoc:$protocVersion:osx-aarch_64"
  const val genGrpc = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
  const val genGrpcMac = "io.grpc:protoc-gen-grpc-java:$grpcVersion:osx-aarch_64"
  const val refineAnnotation = "dev.rikka.tools.refine:annotation:$hiddenApiRefineVersion"
  const val refineRuntime = "dev.rikka.tools.refine:runtime:$hiddenApiRefineVersion"

  const val protobufPlugin = "com.google.protobuf:protobuf-gradle-plugin:0.8.18"
  const val kotlinterPlugin = "org.jmailen.gradle:kotlinter-gradle:3.9.0"
  const val hiddenApiRefinePlugin = "dev.rikka.tools.refine:gradle-plugin:$hiddenApiRefineVersion"
  const val kspPlugin = "com.google.devtools.ksp:symbol-processing-gradle-plugin:1.6.10-1.0.4"

  const val roomCompiler = "androidx.room:room-compiler:$roomVersion"
  const val moshiCompiler = "com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion"
  const val refineAnnotationCompiler = "dev.rikka.tools.refine:annotation-processor:$hiddenApiRefineVersion"

  val tests = arrayOf(
    "junit:junit:4.13.2"
  )
  val androidTests = arrayOf(
    "androidx.test.ext:junit:1.1.3",
    "androidx.test.espresso:espresso-core:3.4.0"
  )
  val lifecycle = arrayOf(
    "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion",
    "androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion",
    "androidx.lifecycle:lifecycle-service:$lifecycleVersion",
    "androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion"
  )
  val room = arrayOf(
    "androidx.room:room-runtime:$roomVersion",
    "androidx.room:room-ktx:$roomVersion"
  )
  val moshi = arrayOf(
    "com.squareup.moshi:moshi:$moshiVersion",
    "com.squareup.retrofit2:converter-moshi:$retrofitVersion"
  )
  val appCenter = arrayOf(
    "com.microsoft.appcenter:appcenter-analytics:$appCenterVersion",
    "com.microsoft.appcenter:appcenter-crashes:$appCenterVersion"
  )
  val grpc = arrayOf(
    "io.grpc:grpc-okhttp:$grpcVersion",
    "io.grpc:grpc-protobuf-lite:$grpcVersion",
    "io.grpc:grpc-stub:$grpcVersion",
    "javax.annotation:javax.annotation-api:1.3.2"
  )
  val myLibs = arrayOf(
    "com.github.zhaobozhen.libraries:me:$myLib",
    "com.github.zhaobozhen.libraries:utils:$myLib",
    "com.github.zhaobozhen.libraries:axml:$myLib",
    // "com.github.zhaobozhen.libraries:dexlib2:$myLib"
  )
}
