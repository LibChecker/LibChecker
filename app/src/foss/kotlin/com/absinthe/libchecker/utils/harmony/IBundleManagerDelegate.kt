@file:Suppress("UNUSED_PARAMETER")

package com.absinthe.libchecker.utils.harmony

import ohos.bundle.BundleInfo
import ohos.bundle.IBundleManager

/**
 * Created by su1216 on 21-6-28.
 */
class IBundleManagerDelegate(applicationDelegate: ApplicationDelegate) {

  private val mIBundleManager: IBundleManager? = applicationDelegate.iBundleManager

  fun getBundleInfo(bundleName: String, flags: Int): BundleInfo? = null
}
