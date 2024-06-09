package com.absinthe.libchecker.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import timber.log.Timber

abstract class BaseFragment<VB : ViewBinding> :
  Fragment(),
  IBinding<VB> {

  private var _binding: VB? = null

  /**
   * You can't call [binding] after [onDestroyView]
   */
  override val binding: VB get() = checkNotNull(_binding) { "Binding has been destroyed" }

  private var parentActivityVisible = false
  private var visible = false
  private var localParentFragment: BaseFragment<VB>? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = inflateBinding(layoutInflater)
    return binding.root
  }

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

  fun isBindingInitialized(): Boolean {
    return _binding != null
  }

  override fun onDestroyView() {
    Timber.d("${javaClass.simpleName} ==> onDestroyView")
    _binding = null
    super.onDestroyView()
  }

  override fun onStart() {
    super.onStart()
    Timber.d("${javaClass.simpleName} ==> onStart")
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

  override fun onStop() {
    super.onStop()
    Timber.d("${javaClass.simpleName} ==> onStop")
  }
}
