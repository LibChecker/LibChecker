package com.absinthe.libchecker.utils

import android.app.Activity
import android.os.Process
import com.absinthe.libchecker.BaseActivity
import java.lang.ref.WeakReference
import java.util.*

object ActivityStackManager {
    /***
     * Get stack
     *
     * @return Activity stack
     */
    /**
     * Activity Stack
     */
    private var stack: Stack<WeakReference<BaseActivity>> = Stack()

    /***
     * Size of Activities
     *
     * @return Size of Activities
     */
    fun stackSize(): Int {
        return stack.size
    }

    /**
     * Add Activity to stack
     */
    fun addActivity(activity: WeakReference<BaseActivity>) {
        stack.add(activity)
    }

    /**
     * Delete Activity
     *
     * @param activity Weak Reference of Activity
     */
    fun removeActivity(activity: WeakReference<BaseActivity>) {
        stack.remove(activity)
    }

    /***
     * Get top stack Activity
     *
     * @return Activity
     */
    val topActivity: BaseActivity?
        get() {
            val activity = stack.lastElement().get()
            return if (null == activity) {
                null
            } else {
                stack.lastElement().get()
            }
        }

    /***
     * Get Activity by class
     *
     * @param cls Activity class
     * @return Activity
     */
    fun getActivity(cls: Class<*>): BaseActivity? {
        var returnActivity: BaseActivity?
        for (activity in stack) {
            activity.get()?.let {
                if (it.javaClass == cls) {
                    returnActivity = activity.get()
                    return returnActivity
                }
            }
        }
        return null
    }

    /**
     * Kill top stack Activity
     */
    fun killTopActivity() {
        try {
            val activity = stack.lastElement()
            killActivity(activity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /***
     * Kill Activity
     *
     * @param activity Activity want to kill
     */
    private fun killActivity(activity: WeakReference<BaseActivity>) {
        try {
            val iterator = stack.iterator()
            while (iterator.hasNext()) {
                val stackActivity = iterator.next()
                if (stackActivity.get() == null) {
                    iterator.remove()
                    continue
                }
                stackActivity.get()?.let {
                    if (it.javaClass.name == activity.get()?.javaClass?.name) {
                        iterator.remove()
                        it.finish()
                        return
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    /***
     * Kill Activity by class
     *
     * @param cls class
     */
    fun killActivity(cls: Class<*>) {
        try {
            val listIterator = stack.listIterator()
            while (listIterator.hasNext()) {
                val activity: Activity? = listIterator.next().get()
                if (activity == null) {
                    listIterator.remove()
                    continue
                }
                if (activity.javaClass == cls) {
                    listIterator.remove()
                    activity.finish()
                    break
                }
            }
        } catch (e: Exception) {
        }
    }

    /**
     * Kill all Activity
     */
    private fun killAllActivity() {
        try {
            val listIterator = stack.listIterator()
            while (listIterator.hasNext()) {
                val activity = listIterator.next().get()
                activity?.finish()
                listIterator.remove()
            }
        } catch (e: Exception) {
        }
    }

    /**
     * Exit application
     */
    fun appExit() {
        killAllActivity()
        Process.killProcess(Process.myPid())
    }
}