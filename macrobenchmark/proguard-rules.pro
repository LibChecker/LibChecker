# Keep test APK shrinking aligned with the release-like benchmark target while
# tolerating runtime-only benchmark dependencies that are absent from R8's
# compile classpath.
-keep class androidx.tracing.** { *; }
-keep class kotlin.** { *; }

-dontwarn androidx.arch.core.executor.ArchTaskExecutor
-dontwarn androidx.arch.core.internal.FastSafeIterableMap
-dontwarn androidx.arch.core.internal.SafeIterableMap$IteratorWithAdditions
-dontwarn androidx.profileinstaller.ProfileInstallReceiver
-dontwarn androidx.startup.Initializer
-dontwarn android.hardware.fingerprint.FingerprintManager
-dontwarn android.hardware.fingerprint.FingerprintManager$AuthenticationCallback
-dontwarn android.hardware.fingerprint.FingerprintManager$AuthenticationResult
-dontwarn android.hardware.fingerprint.FingerprintManager$CryptoObject
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.MustBeClosed
-dontwarn com.squareup.moshi.JsonAdapter
-dontwarn com.squareup.moshi.JsonClass
-dontwarn com.squareup.moshi.JsonDataException
-dontwarn com.squareup.moshi.JsonReader$Options
-dontwarn com.squareup.moshi.JsonReader
-dontwarn com.squareup.moshi.JsonWriter
-dontwarn com.squareup.moshi.Moshi$Builder
-dontwarn com.squareup.moshi.Moshi
-dontwarn com.squareup.moshi.Types
-dontwarn com.squareup.moshi.internal.Util
