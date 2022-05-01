package com.absinthe.libchecker.utils.harmony

/**
 * Created by su1216 on 21-6-28.
 */
object HarmonyOsUtil {

  private val _isHarmonyOs by lazy {
    runCatching {
      val clz = Class.forName("com.huawei.system.BuildEx")
      val method = clz.getMethod("getOsBrand")
      "harmony".equals(method.invoke(clz) as String?, ignoreCase = true)
    }.getOrDefault(false)
  }

  fun <T> wrapperStub(f: () -> T): T? {
    return try {
      f()
    } catch (e: Throwable) {
      null
    }
  }

  fun isHarmonyOs(): Boolean = _isHarmonyOs
}
