package com.absinthe.libchecker.constant

import com.absinthe.libchecker.ui.fragment.applist.MODE_SORT_BY_LIB
import com.absinthe.libchecker.utils.SPUtils
import com.blankj.utilcode.util.Utils

object GlobalValues {
    var sortMode: Int = SPUtils.getInt(Utils.getApp(), Const.PREF_SORT_MODE, MODE_SORT_BY_LIB)
        get() = SPUtils.getInt(Utils.getApp(), Const.PREF_SORT_MODE, MODE_SORT_BY_LIB)
        set(value) {
            field = value
            SPUtils.putInt(Utils.getApp(), Const.PREF_SORT_MODE, value)
        }
}