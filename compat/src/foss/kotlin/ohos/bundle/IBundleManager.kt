@file:Suppress("UNUSED_PARAMETER")

package ohos.bundle

object IBundleManager {
  const val GET_BUNDLE_DEFAULT: Int = 0
  const val GET_BUNDLE_WITH_ABILITIES: Int = 0

  fun getBundleInfo(bundleName: String, flags: Int): BundleInfo {
    return BundleInfo
  }
}
