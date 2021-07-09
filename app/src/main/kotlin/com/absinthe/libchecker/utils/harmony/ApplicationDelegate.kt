package com.absinthe.libchecker.utils.harmony

import android.content.Context
import ohos.bundle.IBundleManager
import timber.log.Timber

/**
 * Created by su1216 on 21-6-28.
 */
class ApplicationDelegate(context: Context) {

    init {
        if (sOhosApplication == null) {
            initApplication(context)
        }
    }

    val iBundleManager: IBundleManager?
        get() {
            return if (ohosContext != null) {
                ohosContext!!.bundleManager
            } else null
        }
    private val ohosContext: ohos.app.Context?
        get() {
            try {
                val clazzOhosApplication = Class.forName("ohos.app.Application")
                val getApplicationContextMethod = clazzOhosApplication.getMethod("getApplicationContext")
                return getApplicationContextMethod.invoke(sOhosApplication) as ohos.app.Context
            } catch (e: Throwable) {
                Timber.w(e)
            }
            return null
        }

    private fun initApplication(context: Context) {
        val applicationContext = context.applicationContext
        val classLoader = applicationContext.classLoader
        try {
            val clazzOhosApplication = Class.forName("ohos.app.Application")
            sOhosApplication = clazzOhosApplication.newInstance()
            val clazzContextDeal = Class.forName("ohos.app.ContextDeal")
            val contextDealConstructor = clazzContextDeal.getConstructor(
                Context::class.java, ClassLoader::class.java
            )
            val contextDeal = contextDealConstructor.newInstance(applicationContext, classLoader)
            val setApplicationMethod =
                clazzContextDeal.getDeclaredMethod("setApplication", clazzOhosApplication)
            setApplicationMethod.invoke(contextDeal, sOhosApplication)
            val attachBaseContextMethod =
                clazzOhosApplication.getMethod("attachBaseContext", ohos.app.Context::class.java)
            attachBaseContextMethod.invoke(sOhosApplication, contextDeal)
        } catch (e: Throwable) {
            Timber.w(e)
        }
    }

    companion object {
        private var sOhosApplication: Any? = null
    }
}
