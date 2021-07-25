package com.absinthe.libchecker.utils.harmony

import ohos.system.version.SystemVersion

/**
 * Created by su1216 on 21-6-28.
 */
object SystemVersionDelegate {

  fun getVersion(): String? = HarmonyOsUtil.wrapperStub { SystemVersion.getVersion() }
}
