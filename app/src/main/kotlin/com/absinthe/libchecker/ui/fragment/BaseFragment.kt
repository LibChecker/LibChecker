package com.absinthe.libchecker.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<T : ViewBinding>(layoutId: Int) : Fragment(layoutId) {
    private var _binding: T? = null
    val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = initBinding(view)
        init()
    }

    abstract fun initBinding(view: View): T
    abstract fun init()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}