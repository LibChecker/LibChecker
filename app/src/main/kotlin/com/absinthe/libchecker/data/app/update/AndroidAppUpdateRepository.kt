package com.absinthe.libchecker.data.app.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import com.absinthe.libchecker.api.request.GetAppUpdateRequest
import com.absinthe.libchecker.domain.app.update.AppSelfUpdatePolicy
import com.absinthe.libchecker.domain.app.update.AppUpdateChannel
import com.absinthe.libchecker.domain.app.update.AppUpdateInstallResult
import com.absinthe.libchecker.domain.app.update.AppUpdateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class AndroidAppUpdateRepository(
  private val context: Context,
  private val packageInstaller: PackageInstaller = context.packageManager.packageInstaller,
  private val request: GetAppUpdateRequest = ApiManager.create(),
  private val okHttpClient: OkHttpClient = ApiManager.okHttpClient,
  private val sdkInt: Int = Build.VERSION.SDK_INT
) : AppUpdateRepository {

  override suspend fun requestUpdateInfo(channel: AppUpdateChannel): GetAppUpdateInfo? {
    return request.requestAppUpdateInfo(channel.requestValue)
  }

  override suspend fun installUpdate(url: String): AppUpdateInstallResult = withContext(Dispatchers.IO) {
    if (!AppSelfUpdatePolicy.isSelfUpdateEnabled(BuildConfig.IS_FOSS, BuildConfig.IS_DEV_VERSION) ||
      !AppSelfUpdatePolicy.supportsUserActionNotRequiredInstall(sdkInt)
    ) {
      return@withContext AppUpdateInstallResult.Unsupported
    }

    try {
      okHttpClient.newCall(url.toApkRequest()).execute().use { response ->
        if (!response.isSuccessful) {
          return@withContext AppUpdateInstallResult.Failure("HTTP ${response.code}")
        }

        val body = response.body
        val sessionId = createSelfUpdateSession(body.contentLength())
        val session = packageInstaller.openSession(sessionId)
        try {
          body.byteStream().use { input ->
            session.openWrite(APK_SESSION_NAME, 0, body.contentLength()).use { output ->
              input.copyTo(output)
              session.fsync(output)
            }
          }
          session.commit(createStatusIntentSender(sessionId))
          AppUpdateInstallResult.Started
        } catch (throwable: Throwable) {
          runCatching { session.abandon() }
          throw throwable
        } finally {
          session.close()
        }
      }
    } catch (throwable: Throwable) {
      if (throwable is CancellationException) {
        throw throwable
      }
      AppUpdateInstallResult.Failure(throwable.message ?: throwable::class.java.simpleName)
    }
  }

  private fun createSelfUpdateSession(sizeBytes: Long): Int {
    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
      setAppPackageName(context.packageName)
      if (sizeBytes > 0) {
        setSize(sizeBytes)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
  }
}
