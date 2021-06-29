package com.absinthe.libchecker.utils.harmony

import android.util.Log
import ohos.bundle.BundleInfo
import ohos.bundle.IBundleManager
import ohos.rpc.RemoteException

/**
 * Created by su1216 on 21-6-28.
 */
class IBundleManagerDelegate(applicationDelegate: ApplicationDelegate) {

    private val mIBundleManager: IBundleManager? = applicationDelegate.iBundleManager

    fun getBundleInfo(bundleName: String, flags: Int): BundleInfo? {
        try {
            return mIBundleManager!!.getBundleInfo(bundleName, flags)
        } catch (e: RemoteException) {
            Log.w("IBundleManagerDelegate", "bundleName: $bundleName", e)
        }
        return null
    }
}
