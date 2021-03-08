package com.absinthe.libchecker.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.BitmapFactory
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.viewmodel.GET_INSTALL_APPS_RETRY_PERIOD
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

private const val SHOOT_CHANNEL_ID = "shoot_channel"
private const val SHOOT_NOTIFICATION_ID = 1
private const val SHOOT_SUCCESS_NOTIFICATION_ID = 2

class ShootService : Service() {

    private val builder by lazy { NotificationCompat.Builder(this, SHOOT_CHANNEL_ID) }
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private val gson = Gson()
    private val repository = LibCheckerApp.repository
    private val listenerList = RemoteCallbackList<OnShootOverListener>()

    private val binder = object : IShootService.Stub() {
        override fun computeSnapshot(dropPrevious: Boolean) {
            Timber.i("computeSnapshot: dropPrevious = $dropPrevious")
            GlobalScope.launch(Dispatchers.IO) { this@ShootService.computeSnapshots(dropPrevious) }
        }

        override fun registerOnShootOverListener(listener: OnShootOverListener?) {
            Timber.i("registerOnShootOverListener $listener")
            listener?.let { listenerList.register(listener) }
        }

        override fun unregisterOnShootOverListener(listener: OnShootOverListener?) {
            Timber.i("unregisterOnShootOverListener $listener")
            listenerList.unregister(listener)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        showNotification()
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        showNotification()
    }

    private fun showNotification() {
        initBuilder()

        notificationManager.apply {
            if (LCAppUtils.atLeastO()) {
                val name = getString(R.string.channel_shoot)
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val mChannel = NotificationChannel(SHOOT_CHANNEL_ID, name, importance)
                createNotificationChannel(mChannel)
            }
            startForeground(SHOOT_NOTIFICATION_ID, builder.build())
        }
    }

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

    private suspend fun computeSnapshots(dropPrevious: Boolean = false) {
        Timber.i("computeSnapshots: dropPrevious = $dropPrevious")
        notificationManager.cancel(SHOOT_SUCCESS_NOTIFICATION_ID)
        initBuilder()

        val ts = System.currentTimeMillis()
        var appList: List<ApplicationInfo>? = AppItemRepository.allApplicationInfoItems.value

        if (appList.isNullOrEmpty()) {
            do {
                appList = try {
                    PackageUtils.getInstallApplications()
                } catch (e: Exception) {
                    delay(GET_INSTALL_APPS_RETRY_PERIOD)
                    null
                }
            } while (appList == null)
        }

        val size = appList.size
        var count = 0
        val dbList = mutableListOf<SnapshotItem>()
        val exceptionInfoList = mutableListOf<ApplicationInfo>()

        builder.setProgress(size, count, false)
        notificationManager.notify(SHOOT_NOTIFICATION_ID, builder.build())

        for (info in appList) {
            try {
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
                            abi = PackageUtils.getAbi(info.sourceDir, info.nativeLibraryDir).toShort(),
                            targetApi = info.targetSdkVersion.toShort(),
                            nativeLibs = gson.toJson(
                                PackageUtils.getNativeDirLibs(info.sourceDir, info.nativeLibraryDir)
                            ),
                            services = gson.toJson(
                                PackageUtils.getComponentStringList(it.packageName, SERVICE, false)
                            ),
                            activities = gson.toJson(
                                PackageUtils.getComponentStringList(it.packageName, ACTIVITY, false)
                            ),
                            receivers = gson.toJson(
                                PackageUtils.getComponentStringList(it.packageName, RECEIVER, false)
                            ),
                            providers = gson.toJson(
                                PackageUtils.getComponentStringList(it.packageName, PROVIDER, false)
                            ),
                            permissions = gson.toJson(PackageUtils.getPermissionsList(it.packageName))
                        )
                    )
                }
                count++
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
                info = exceptionInfoList[0]
                abiValue = PackageUtils.getAbi(info.sourceDir, info.nativeLibraryDir)
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
                            nativeLibs = gson.toJson(PackageUtils.getNativeDirLibs(info.sourceDir, info.nativeLibraryDir, PackageUtils.is32bit(abiValue))),
                            services = gson.toJson(PackageUtils.getComponentStringList(it.packageName, SERVICE, false)),
                            activities = gson.toJson(PackageUtils.getComponentStringList(it.packageName, ACTIVITY, false)),
                            receivers = gson.toJson(PackageUtils.getComponentStringList(it.packageName, RECEIVER, false)),
                            providers = gson.toJson(PackageUtils.getComponentStringList(it.packageName, PROVIDER, false)),
                            permissions = gson.toJson(PackageUtils.getPermissionsList(it.packageName))
                        )
                    )
                }
                exceptionInfoList.removeAt(0)
            } catch (e: Exception) {
                exceptionInfoList.removeAt(0)
                continue
            }
            count++
        }

        builder.setProgress(size, count, false)
        notificationManager.notify(SHOOT_NOTIFICATION_ID, builder.build())
        repository.insertSnapshots(dbList)
        repository.insert(TimeStampItem(ts))

        if (dropPrevious) {
            Timber.i("deleteSnapshotsAndTimeStamp: ${GlobalValues.snapshotTimestamp}")
            repository.deleteSnapshotsAndTimeStamp(GlobalValues.snapshotTimestamp)
        }

        builder.setProgress(0, 0, false)
            .setOngoing(false)
            .setContentTitle(getString(R.string.noti_shoot_title_saved))
            .setContentText(getFormatDateString(ts))
        notificationManager.notify(SHOOT_SUCCESS_NOTIFICATION_ID, builder.build())

        GlobalValues.snapshotTimestamp = ts
        notifyFinished(ts)
        stopForeground(true)
        stopSelf()
        Timber.i("computeSnapshots end")
    }

    private fun getFormatDateString(timestamp: Long): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.getDefault())
        val date = Date(timestamp)
        return simpleDateFormat.format(date)
    }

    private fun initBuilder() {
        builder.setContentTitle(getString(R.string.noti_shoot_title))
            .setSmallIcon(R.drawable.ic_logo)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(0, 0, true)
            .setSilent(true)
            .setOngoing(true)
            .setAutoCancel(false)
    }
}