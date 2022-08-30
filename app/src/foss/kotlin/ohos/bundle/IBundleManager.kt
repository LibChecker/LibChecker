@file:Suppress("ALL")

package ohos.bundle

object IBundleManager {
  val GET_BUNDLE_DEFAULT: Int = 0
  val GET_BUNDLE_WITH_ABILITIES: Int = 0

  fun getBundleInfo(bundleName: String, flags: Int): BundleInfo {
    return BundleInfo
  }
}
