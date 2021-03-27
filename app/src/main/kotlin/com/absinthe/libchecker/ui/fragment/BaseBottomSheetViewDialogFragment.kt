package com.absinthe.libchecker.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libraries.utils.utils.UiUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class BaseBottomSheetViewDialogFragment<T: View> : BottomSheetDialogFragment() {

    private var _root: T? = null
    val root get() = _root!!

    abstract fun initRootView(): T
    abstract fun init()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _root = initRootView()
        init()
        return _root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            it.findViewById<View>(com.google.android.material.R.id.container).fitsSystemWindows = false
            UiUtils.setSystemBarStyle(it)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.DialogAnimation
    }

    override fun onDestroyView() {
        _root = null
        super.onDestroyView()
    }
}