package com.absinthe.libchecker.data.app.update

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import com.absinthe.libchecker.api.request.GetAppUpdateRequest
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.app.update.AppSelfUpdatePolicy
import com.absinthe.libchecker.domain.app.update.AppUpdateChannel
import com.absinthe.libchecker.domain.app.update.AppUpdateInstallResult
import com.absinthe.libchecker.domain.app.update.AppUpdateRepository
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import retrofit2.HttpException

class AndroidAppUpdateRepository(
  private val context: Context,
  private val packageInstaller: PackageInstaller = context.packageManager.packageInstaller,
  private val request: GetAppUpdateRequest = ApiManager.create(),
  private val okHttpClient: OkHttpClient = ApiManager.okHttpClient,
  private val sdkInt: Int = Build.VERSION.SDK_INT
) : AppUpdateRepository {

  override suspend fun requestUpdateInfo(channel: AppUpdateChannel): GetAppUpdateInfo? {
    val requestValue = channel.requestValue
    val authorization = GlobalValues.githubApiAuthorizationHeaderFor(ApiManager.ASSETS_REPO_BASE_URL)
    return try {
      request.requestAppUpdateInfo(requestValue, authorization)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      if (
        e !is IOException &&
        e !is HttpException &&
        e !is JsonDataException &&
        e !is JsonEncodingException
      ) {
        throw e
      }
      request.requestFallbackAppUpdateInfo(requestValue)
    }
  }

  override suspend fun installUpdate(url: String): AppUpdateInstallResult = withContext(Dispatchers.IO) {
    if (!AppSelfUpdatePolicy.isSelfUpdateEnabled(BuildConfig.IS_FOSS, BuildConfig.IS_DEV_VERSION)) {
      return@withContext AppUpdateInstallResult.Unsupported
    }

    val installWithoutUserAction = AppSelfUpdatePolicy.supportsUserActionNotRequiredInstall(sdkInt)
    if (installWithoutUserAction && !hasUserActionNotRequiredInstallPermission()) {
      return@withContext AppUpdateInstallResult.Unsupported
    }

    try {
      okHttpClient.newCall(url.toApkRequest()).execute().use { response ->
        if (!response.isSuccessful) {
          return@withContext AppUpdateInstallResult.Failure("HTTP ${response.code}")
        }

        val body = response.body
        val contentLength = body.contentLength()
        if (installWithoutUserAction) {
          installWithPackageInstaller(body, contentLength)
        } else {
          installWithSystemInstaller(body)
        }
      }
    } catch (throwable: Throwable) {
      if (throwable is CancellationException) {
        throw throwable
      }
      AppUpdateInstallResult.Failure(throwable.message ?: throwable::class.java.simpleName)
    }
  }

  private fun installWithPackageInstaller(body: ResponseBody, sizeBytes: Long): AppUpdateInstallResult {
    val sessionId = createSelfUpdateSession(sizeBytes)
    val session = packageInstaller.openSession(sessionId)
    try {
      body.byteStream().use { input ->
        session.openWrite(APK_SESSION_NAME, 0, sizeBytes).use { output ->
          input.copyTo(output)
          session.fsync(output)
        }
      }
      session.commit(createStatusIntentSender(sessionId))
      return AppUpdateInstallResult.Started
    } catch (throwable: Throwable) {
      runCatching { session.abandon() }
      throw throwable
    } finally {
      session.close()
    }
  }

  @Suppress("DEPRECATION")
  private fun installWithSystemInstaller(body: ResponseBody): AppUpdateInstallResult {
    val targetDir = File(context.cacheDir, UPDATE_CACHE_DIR_NAME)
    if (!targetDir.exists() && !targetDir.mkdirs()) {
      return AppUpdateInstallResult.Failure("Failed to create ${targetDir.absolutePath}")
    }

    val targetFile = File(targetDir, APK_SESSION_NAME)
    val tempFile = File(targetDir, "$APK_SESSION_NAME.tmp")
    if (tempFile.exists()) {
      tempFile.delete()
    }
    body.byteStream().use { input ->
      tempFile.outputStream().use { output ->
        input.copyTo(output)
      }
    }
    if (targetFile.exists()) {
      targetFile.delete()
    }
    if (!tempFile.renameTo(targetFile)) {
      tempFile.copyTo(targetFile, overwrite = true)
      tempFile.delete()
    }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
    val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
      setDataAndType(uri, MIMETYPE_APK)
      putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
      context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
      return AppUpdateInstallResult.Failure(e.message ?: e::class.java.simpleName)
    }
    return AppUpdateInstallResult.Started
  }

  private fun hasUserActionNotRequiredInstallPermission(): Boolean {
    return context.checkSelfPermission(UPDATE_PACKAGES_WITHOUT_USER_ACTION) == PackageManager.PERMISSION_GRANTED
  }

  private fun createSelfUpdateSession(sizeBytes: Long): Int {
    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
      setAppPackageName(context.packageName)
      if (sizeBytes > 0) {
        setSize(sizeBytes)
      }
      if (sdkInt >= Build.VERSION_CODES.S) {
        setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
      }
    }
    return packageInstaller.createSession(params)
  }

  private fun createStatusIntentSender(sessionId: Int) = PendingIntent.getBroadcast(
    context,
    sessionId,
    Intent(context, AppUpdateInstallResultReceiver::class.java)
      .setPackage(context.packageName)
      .putExtra(AppUpdateInstallResultReceiver.EXTRA_SESSION_ID, sessionId),
    PendingIntent.FLAG_UPDATE_CURRENT or mutablePendingIntentFlag()
  ).intentSender

  private fun mutablePendingIntentFlag(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      PendingIntent.FLAG_MUTABLE
    } else {
      0
    }
  }

  private fun String.toApkRequest(): Request {
    return Request.Builder()
      .url(this)
      .header("Referer", context.packageName)
      .build()
  }

  private val AppUpdateChannel.requestValue: String
    get() = when (this) {
      AppUpdateChannel.STABLE -> "stable"
      AppUpdateChannel.CI -> "ci"
    }

  private companion object {
    const val APK_SESSION_NAME = "base.apk"
    const val MIMETYPE_APK = "application/vnd.android.package-archive"
    const val UPDATE_CACHE_DIR_NAME = "app_update"
    const val UPDATE_PACKAGES_WITHOUT_USER_ACTION = "android.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION"
  }
}
