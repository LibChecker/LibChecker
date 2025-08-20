package android.content.pm

import dev.rikka.tools.refine.RefineAs

@RefineAs(PackageInfo::class)
class PackageInfoHidden {

  var overlayTarget: String? = null

  fun isOverlayPackage(): Boolean {
    throw RuntimeException("Stub")
  }
}