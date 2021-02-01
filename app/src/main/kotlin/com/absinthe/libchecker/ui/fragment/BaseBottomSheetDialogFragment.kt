package com.absinthe.libchecker.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.R
import com.absinthe.libraries.utils.utils.UiUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class BaseBottomSheetDialogFragment<T : ViewBinding> : BottomSheetDialogFragment() {

    private var _binding: T? = null
    val binding get() = _binding!!

    abstract fun initBinding(): T
    abstract fun init()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = initBinding()
        init()
        return _binding!!.root
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

    override fun onStop() {
        super.onStop()
        dialog?.show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}