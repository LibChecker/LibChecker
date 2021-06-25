package com.absinthe.libchecker.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import timber.log.Timber

abstract class BaseFragment<T : ViewBinding>(layoutId: Int) : Fragment(layoutId) {

    private var _binding: T? = null
    val binding get() = _binding!!

    private var parentActivityVisible = false
    private var visible = false
    private var localParentFragment: BaseFragment<T>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("${javaClass.simpleName} ==> onViewCreated")
        _binding = initBinding(view)
        init()
    }

    abstract fun initBinding(view: View): T
    abstract fun init()

    open fun onVisibilityChanged(visible: Boolean) {
        Timber.d("${javaClass.simpleName} ==> onVisibilityChanged = $visible")
    }

    override fun onDestroyView() {
        Timber.d("${javaClass.simpleName} ==> onDestroyView")
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("${javaClass.simpleName} ==> onResume")
        onVisibilityChanged(true)
    }

    override fun onPause() {
        super.onPause()
        Timber.d("${javaClass.simpleName} ==> onPause")
        onVisibilityChanged(false)
    }

}