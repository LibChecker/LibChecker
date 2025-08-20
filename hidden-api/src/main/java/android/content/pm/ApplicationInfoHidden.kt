package android.content.pm

import dev.rikka.tools.refine.RefineAs

@RefineAs(ApplicationInfo::class)
class ApplicationInfoHidden {

  companion object {
    const val PRIVATE_FLAG_HIDDEN = 1
  }

  var primaryCpuAbi: String? = null
  var privateFlags: Int = 0
}