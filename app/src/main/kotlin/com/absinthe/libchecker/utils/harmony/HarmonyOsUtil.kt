package com.absinthe.libchecker.utils.harmony

/**
 * Created by su1216 on 21-6-28.
 */
object HarmonyOsUtil {

  private var sHarmonyOs: Boolean? = null

  fun <T> wrapperStub(f: () -> T): T? {
    return try {
      f()
    } catch (e: Throwable) {
      null
    }
  }

  fun isHarmonyOs(): Boolean {
    if (sHarmonyOs != null) {
      return sHarmonyOs!!
    }
    return try {
      Class.forName("ohos.app.Application")
      Class.forName("ohos.system.version.SystemVersion")
      sHarmonyOs = true
      sHarmonyOs!!
    } catch (e: ClassNotFoundException) {
      sHarmonyOs = false
      sHarmonyOs!!
    }
  }
}
