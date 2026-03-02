package com.absinthe.libchecker.utils.harmony

import ohos.bundle.BundleInfo
import ohos.bundle.IBundleManager
import ohos.rpc.RemoteException
import timber.log.Timber

/**
 * Created by su1216 on 21-6-28.
 */
class IBundleManagerDelegate(applicationDelegate: ApplicationDelegate) {

  private val mIBundleManager: IBundleManager? = applicationDelegate.iBundleManager

  fun getBundleInfo(bundleName: String, flags: Int): BundleInfo? {
    try {
      return mIBundleManager!!.getBundleInfo(bundleName, flags)
    } catch (e: RemoteException) {
      Timber.w(e, "bundleName: $bundleName")
    }
    return null
  }
}
