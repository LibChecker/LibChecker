package com.absinthe.libchecker.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.ui.main.MainActivity
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.toJson
import com.absinthe.libraries.utils.manager.TimeRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SHOOT_CHANNEL_ID = "shoot_channel"
private const val SHOOT_NOTIFICATION_ID = 1
private const val SHOOT_SUCCESS_NOTIFICATION_ID = 2

const val ACTION_SHOOT_AND_STOP_AUTO = "action_shoot_and_stop_auto"
const val EXTRA_DROP_PREVIOUS = "extra_drop_previous"

class ShootService : LifecycleService() {

  private val builder by lazy { NotificationCompat.Builder(this, SHOOT_CHANNEL_ID) }
  private val notificationManager by lazy { NotificationManagerCompat.from(this) }
  private val configuration by lazy {
    Configuration(resources.configuration).apply {
      setLocale(GlobalValues.locale)
    }
  }
  private val repository = Repositories.lcRepository
  private val listenerList = RemoteCallbackList<OnShootListener>()

  private val binder by lazy { ShootBinder(this, lifecycleScope) }

  private var _isShooting: Boolean = false

  override fun onBind(intent: Intent): IBinder {
    super.onBind(intent)
    Timber.d("onBind")
    return binder
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Timber.d("onStartCommand")
    if (intent?.`package` != packageName) {
      stopForeground(true)
      stopSelf()
    }
    if (intent?.action == ACTION_SHOOT_AND_STOP_AUTO) {
      val dropPrevious = intent.getBooleanExtra(EXTRA_DROP_PREVIOUS, false)
      lifecycleScope.launch(Dispatchers.IO) {
        computeSnapshots(dropPrevious, true)
      }
    } else {
      stopForeground(true)
      stopSelf()
    }
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    super.onDestroy()
    Timber.d("onDestroy")
    stopForeground(true)
  }

  private fun showNotification() {
    initBuilder()

    notificationManager.apply {
      if (LCAppUtils.atLeastO()) {
        val name = createConfigurationContext(configuration).resources
          .getString(R.string.channel_shoot)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(SHOOT_CHANNEL_ID, name, importance)
        createNotificationChannel(channel)
      }
      startForeground(SHOOT_NOTIFICATION_ID, builder.build())
    }
  }

  @Synchronized
  private fun notifyFinished(timestamp: Long) {
    Timber.i("notifyFinished start")
    val count = listenerList.beginBroadcast()
    for (i in 0 until count) {
      try {
        Timber.i("notifyFinished $i")
        listenerList.getBroadcastItem(i).onShootFinished(timestamp)
      } catch (e: RemoteException) {
        Timber.e(e)
      }
    }
    listenerList.finishBroadcast()
  }

  @Synchronized
  private fun notifyProgress(progress: Int) {
    val count = listenerList.beginBroadcast()
    for (i in 0 until count) {
      try {
        listenerList.getBroadcastItem(i).onProgressUpdated(progress)
      } catch (e: RemoteException) {
        Timber.e(e)
      }
    }
    listenerList.finishBroadcast()
  }

  private suspend fun computeSnapshots(
    dropPrevious: Boolean = false,
    stopWhenFinish: Boolean = false
  ) {
    if (isComputing) {
      Timber.w("computeSnapshots isComputing, ignored")
      return
    }
    isComputing = true
    Timber.i("computeSnapshots: dropPrevious = $dropPrevious")
    _isShooting = true
    notificationManager.cancel(SHOOT_SUCCESS_NOTIFICATION_ID)
    showNotification()
    notificationManager.notify(SHOOT_NOTIFICATION_ID, builder.build())

    val timer = TimeRecorder().also {
      it.start()
    }
    val ts = System.currentTimeMillis()
    val appList = PackageUtils.getAppsList()

    repository.deleteAllSnapshotDiffItems()

    val size = appList.size
    var count = 0
    val dbList = mutableListOf<SnapshotItem>()
    val exceptionInfoList = mutableListOf<PackageInfo>()

    builder.setProgress(size, count, false)
    notificationManager.notify(SHOOT_NOTIFICATION_ID, builder.build())

    var currentProgress: Int
    var lastProgress = 0
    var ai: ApplicationInfo

    for (info in appList) {
      try {
        ai = info.applicationInfo
        dbList.add(
          SnapshotItem(
            id = null,
            packageName = info.packageName,
            timeStamp = ts,
            label = ai.loadLabel(packageManager).toString(),
            versionName = info.versionName ?: "null",
            versionCode = PackageUtils.getVersionCode(info),
            installedTime = info.firstInstallTime,
            lastUpdatedTime = info.lastUpdateTime,
            isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
            abi = PackageUtils.getAbi(info).toShort(),
            targetApi = ai.targetSdkVersion.toShort(),
            nativeLibs = PackageUtils.getNativeDirLibs(info).toJson().orEmpty(),
            services = PackageUtils.getComponentStringList(info.packageName, SERVICE, false)
              .toJson().orEmpty(),
            activities = PackageUtils.getComponentStringList(info.packageName, ACTIVITY, false)
              .toJson().orEmpty(),
            receivers = PackageUtils.getComponentStringList(info.packageName, RECEIVER, false)
              .toJson().orEmpty(),
            providers = PackageUtils.getComponentStringList(info.packageName, PROVIDER, false)
              .toJson().orEmpty(),
            permissions = PackageUtils.getPermissionsList(info.packageName).toJson().orEmpty(),
            metadata = PackageUtils.getMetaDataItems(info).toJson().orEmpty(),
            packageSize = PackageUtils.getPackageSize(info, true)
          )
        )
        count++
        currentProgress = count * 100 / size
        if (currentProgress > lastProgress) {
          lastProgress = currentProgress
          notifyProgress(currentProgress)
        }
      } catch (e: Exception) {
        Timber.e(e)
        exceptionInfoList.add(info)
        continue
      }

      if (dbList.size >= 50) {
        builder.setProgress(size, count, false)
        notificationManager.notify(SHOOT_NOTIFICATION_ID, builder.build())
        repository.insertSnapshots(dbList)
        dbList.clear()
      }
    }

    var info: ApplicationInfo
    var abiValue: Int
    while (exceptionInfoList.isNotEmpty()) {
      try {
        info = exceptionInfoList[0].applicationInfo
        abiValue = PackageUtils.getAbi(exceptionInfoList[0])
        PackageUtils.getPackageInfo(info.packageName).let {
          dbList.add(
            SnapshotItem(
              id = null,
              packageName = it.packageName,
              timeStamp = ts,
              label = info.loadLabel(packageManager).toString(),
              versionName = it.versionName ?: "null",
              versionCode = PackageUtils.getVersionCode(it),
              installedTime = it.firstInstallTime,
              lastUpdatedTime = it.lastUpdateTime,
              isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
              abi = abiValue.toShort(),
              targetApi = info.targetSdkVersion.toShort(),
              nativeLibs = PackageUtils.getNativeDirLibs(it).toJson().orEmpty(),
              services = PackageUtils.getComponentStringList(it.packageName, SERVICE, false)
                .toJson().orEmpty(),
              activities = PackageUtils.getComponentStringList(it.packageName, ACTIVITY, false)
                .toJson().orEmpty(),
              receivers = PackageUtils.getComponentStringList(it.packageName, RECEIVER, false)
                .toJson().orEmpty(),
              providers = PackageUtils.getComponentStringList(it.packageName, PROVIDER, false)
                .toJson().orEmpty(),
              permissions = PackageUtils.getPermissionsList(it.packageName).toJson().orEmpty(),
              metadata = PackageUtils.getMetaDataItems(it).toJson().orEmpty(),
              packageSize = PackageUtils.getPackageSize(it, true)
            )
          )
        }
        exceptionInfoList.removeAt(0)
      } catch (e: Exception) {
        exceptionInfoList.removeAt(0)
        continue
      }
      count++
      notifyProgress(count * 100 / size)
    }

    builder.setProgress(size, count, false)
    notificationManager.notify(SHOOT_NOTIFICATION_ID, builder.build())
    repository.insertSnapshots(dbList)
    repository.insert(TimeStampItem(ts, null))

    if (dropPrevious) {
      Timber.i("deleteSnapshotsAndTimeStamp: ${GlobalValues.snapshotTimestamp}")
      repository.deleteSnapshotsAndTimeStamp(GlobalValues.snapshotTimestamp)
    }

    notificationManager.cancel(SHOOT_NOTIFICATION_ID)

    builder.setProgress(0, 0, false)
      .setOngoing(false)
      .setContentTitle(createConfigurationContext(configuration).resources.getString(R.string.noti_shoot_title_saved))
      .setContentText(getFormatDateString(ts))
    notificationManager.notify(SHOOT_SUCCESS_NOTIFICATION_ID, builder.build())

    timer.end()
    Timber.d("computeSnapshots: $timer")

    GlobalValues.snapshotTimestamp = ts
    _isShooting = false
    notifyFinished(ts)
    stopForeground(true)
    stopSelf()
    Timber.i("computeSnapshots end")
    isComputing = false

    if (stopWhenFinish) {
      stopSelf()
    }
  }

  private fun getFormatDateString(timestamp: Long): String {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.getDefault())
    val date = Date(timestamp)
    return simpleDateFormat.format(date)
  }

  private fun initBuilder() {
    val pi = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java).also {
        it.action = Constants.ACTION_SNAPSHOT
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      },
      PendingIntent.FLAG_IMMUTABLE
    )
    builder.setContentTitle(createConfigurationContext(configuration).resources.getString(R.string.noti_shoot_title))
      .setSmallIcon(R.drawable.ic_logo)
      .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
      .setColor(R.color.colorPrimary.getColor(this))
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setContentIntent(pi)
      .setProgress(0, 0, true)
      .setSilent(true)
      .setOngoing(true)
      .setAutoCancel(false)
  }

  companion object {
    var isComputing = false
  }

  class ShootBinder(service: ShootService, private val lifecycleScope: CoroutineScope) :
    IShootService.Stub() {

    private val serviceRef: WeakReference<ShootService> = WeakReference(service)

    override fun computeSnapshot(dropPrevious: Boolean) {
      Timber.i("computeSnapshot: dropPrevious = $dropPrevious")
      lifecycleScope.launch(Dispatchers.IO) {
        serviceRef.get()?.computeSnapshots(dropPrevious)
      }
    }

    override fun isShooting(): Boolean {
      return serviceRef.get()?._isShooting ?: false
    }

    override fun registerOnShootOverListener(listener: OnShootListener?) {
      Timber.i("registerOnShootOverListener $listener")
      listener?.let {
        serviceRef.get()?.listenerList?.register(listener)
      }
    }

    override fun unregisterOnShootOverListener(listener: OnShootListener?) {
      Timber.i("unregisterOnShootOverListener $listener")
      serviceRef.get()?.listenerList?.unregister(listener)
    }
  }
}
