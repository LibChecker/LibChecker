package com.absinthe.libchecker.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.base.BaseActivity.Companion.inflateBinding
import timber.log.Timber

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

  protected lateinit var binding: VB

  private var parentActivityVisible = false
  private var visible = false
  private var localParentFragment: BaseFragment<VB>? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    Timber.d("${javaClass.simpleName} ==> onViewCreated")
    init()
  }

  abstract fun init()

  open fun onVisibilityChanged(visible: Boolean) {
    Timber.d("${javaClass.simpleName} ==> onVisibilityChanged = $visible")
    this.visible = visible
  }

  fun isFragmentVisible(): Boolean {
    return visible
  }

  @CallSuper
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = inflateBinding(layoutInflater)
  }

  @CallSuper
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = binding.root

  override fun onDestroyView() {
    Timber.d("${javaClass.simpleName} ==> onDestroyView")
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
