package com.absinthe.libchecker.ui.fragment

import java.lang.ref.WeakReference

/**
 * <pre>
 * author : Absinthe
 * time : 2020/08/28
 * </pre>
 */
interface IListController {
    fun onReturnTop()

    companion object {
        var controller: WeakReference<IListController>? = null
    }
}