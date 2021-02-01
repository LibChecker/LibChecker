package com.absinthe.libchecker.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.extensions.logd

abstract class BaseFragment<T : ViewBinding>(layoutId: Int) : Fragment(layoutId) {

    private var _binding: T? = null
    val binding get() = _binding!!

    private var parentActivityVisible = false
    private var visible = false
    private var localParentFragment: BaseFragment<T>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = initBinding(view)
        init()
    }

    abstract fun initBinding(view: View): T
    abstract fun init()

    open fun onVisibilityChanged(visible: Boolean) {
        logd("==> onVisibilityChanged = $visible")
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        onVisibilityChanged(true)
    }

    override fun onPause() {
        super.onPause()
        onVisibilityChanged(false)
    }

}