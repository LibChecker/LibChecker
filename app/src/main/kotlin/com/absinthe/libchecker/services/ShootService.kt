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
import com.absinthe.libchecker.extensions.loge
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.viewmodel.GET_INSTALL_APPS_RETRY_PERIOD
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SHOOT_CHANNEL_ID = "shoot_channel"
private const val SHOOT_NOTIFICATION_ID = 1

class ShootService : Service() {

    private val builder by lazy { NotificationCompat.Builder(this, SHOOT_CHANNEL_ID) }
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private val gson = Gson()
    private val repository = LibCheckerApp.repository
    private val listenerList = RemoteCallbackList<OnShootOverListener>()

    private val binder = object : IShootService.Stub() {
        override fun computeSnapshot(dropPrevious: Boolean) {
            GlobalScope.launch { this@ShootService.computeSnapshots(dropPrevious) }
        }

        override fun registerOnShootOverListener(listener: OnShootOverListener?) {
            listener?.let { listenerList.register(listener) }
        }

        override fun unregisterOnShootOverListener(listener: OnShootOverListener?) {
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
        builder.setContentTitle(getString(R.string.noti_shoot_title))
            .setSmallIcon(R.drawable.ic_logo)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setAutoCancel(false)

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
        val count = listenerList.beginBroadcast()
        for (i in 0 until count) {
            try {
                listenerList.getBroadcastItem(i).onShootFinished(timestamp)
            } catch (e: RemoteException) {
                loge(e.toString())
            }
        }
        listenerList.finishBroadcast()
    }

    private suspend fun computeSnapshots(dropPrevious: Boolean = false) {
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
                builder.setProgress(size, count, false)
                notificationManager.notify(SHOOT_NOTIFICATION_ID, builder.build())
            } catch (e: Exception) {
                loge(e.toString())
                exceptionInfoList.add(info)
                continue
            }

            if (dbList.size >= 50) {
                repository.insertSnapshots(dbList)
                dbList.clear()
            }
        }

        var info: ApplicationInfo
        while (exceptionInfoList.isNotEmpty()) {
            try {
                info = exceptionInfoList[0]
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
                            nativeLibs = gson.toJson(PackageUtils.getNativeDirLibs(info.sourceDir, info.nativeLibraryDir)),
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
            builder.setProgress(size, count, false)
            notificationManager.notify(SHOOT_NOTIFICATION_ID, builder.build())
        }

        repository.insertSnapshots(dbList)
        repository.insert(TimeStampItem(ts))

        if (dropPrevious) {
            repository.deleteSnapshotsAndTimeStamp(GlobalValues.snapshotTimestamp)
        }

        GlobalValues.snapshotTimestamp = ts
        notifyFinished(ts)
        stopForeground(true)
        stopSelf()
    }
}