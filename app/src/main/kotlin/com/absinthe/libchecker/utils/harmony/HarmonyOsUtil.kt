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
    return try {
      val clz = Class.forName("com.huawei.system.BuildEx")
      val method = clz.getMethod("getOsBrand")
      return "harmony".equals(method.invoke(clz) as String?, ignoreCase = true)
    } catch (e: Exception) {
      false
    }
  }
}
