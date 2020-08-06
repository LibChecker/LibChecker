package com.absinthe.libchecker.utils

import android.view.View
import androidx.annotation.IntRange
import com.absinthe.libchecker.R

/**
 * 防抖动点击
 *
 * @author jiangshicheng
 */
object AntiShakeUtils {
    private const val INTERNAL_TIME: Long = 1000

    /**
     * Whether this click event is invalid.
     *
     * @param target       target view
     * @param internalTime the internal time. The unit is millisecond.
     * @return true, invalid click event.
     */
    fun isInvalidClick(
        target: View,
        @IntRange(from = 0) internalTime: Long = INTERNAL_TIME
    ): Boolean {
        val curTimeStamp = System.currentTimeMillis()
        val o = target.getTag(R.id.last_click_time)

        if (o == null) {
            target.setTag(R.id.last_click_time, curTimeStamp)
            return false
        }

        val lastClickTimeStamp = o as Long
        val isInvalid = curTimeStamp - lastClickTimeStamp < internalTime

        if (!isInvalid) {
            target.setTag(R.id.last_click_time, curTimeStamp)
        }
        return isInvalid
    }
}