package com.absinthe.libchecker.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libraries.utils.utils.UiUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class BaseBottomSheetViewDialogFragment<T: View> : BottomSheetDialogFragment() {

    private var _root: T? = null
    private val behavior by lazy { BottomSheetBehavior.from(root.parent as View) }
    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                getHeaderView().onHandlerActivated(true)
            } else if (newState == BottomSheetBehavior.STATE_SETTLING) {
                getHeaderView().onHandlerActivated(false)
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    val root get() = _root!!

    abstract fun initRootView(): T
    abstract fun init()
    abstract fun getHeaderView(): BottomSheetHeaderView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _root = initRootView()
        init()
        return _root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            it.attributes?.windowAnimations = R.style.DialogAnimation
            it.findViewById<View>(com.google.android.material.R.id.container).fitsSystemWindows = false
            UiUtils.setSystemBarStyle(it)
        }
        behavior.addBottomSheetCallback(bottomSheetCallback)
    }

    override fun onStop() {
        super.onStop()
        behavior.removeBottomSheetCallback(bottomSheetCallback)
        dismiss()
    }

    override fun onDestroyView() {
        _root = null
        super.onDestroyView()
    }
}