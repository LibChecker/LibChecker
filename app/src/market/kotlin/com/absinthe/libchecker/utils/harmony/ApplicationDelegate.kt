package com.absinthe.libchecker.utils.harmony

import android.content.Context
import ohos.app.Application
import ohos.app.ContextDeal
import ohos.bundle.IBundleManager
import timber.log.Timber

/**
 * Created by su1216 on 21-6-28.
 */
class ApplicationDelegate(val context: Context) {

  private fun initApplication() {
    val applicationContext = context.applicationContext
    val classLoader = applicationContext.classLoader
    try {
      sOhosApplication = Application()
      val contextDeal = ContextDeal(applicationContext, classLoader)
      if (!setApplication(contextDeal, sOhosApplication!!)) {
        contextDeal.setApplication(sOhosApplication)
      }
      sOhosApplication!!.attachBaseContext(contextDeal)
    } catch (e: Throwable) {
      Timber.w(e)
    }
  }

  private fun setApplication(contextDeal: ContextDeal, application: Application): Boolean {
    val fields = ContextDeal::class.java.declaredFields
    for (field in fields) {
      if (field.type == Application::class.java) {
        field.isAccessible = true
        field.set(contextDeal, application)
        return true
      }
    }
    return false
  }

  val iBundleManager: IBundleManager?
    get() {
      return if (ohosContext != null) {
        ohosContext!!.bundleManager
      } else {
        null
      }
    }
  val ohosContext: ohos.app.Context?
    get() {
      try {
        return sOhosApplication!!.context
      } catch (e: Throwable) {
        Timber.w(e)
      }
      return null
    }

  companion object {
    private var sOhosApplication: Application? = null
  }

  init {
    if (sOhosApplication == null) {
      initApplication()
    }
  }
}
