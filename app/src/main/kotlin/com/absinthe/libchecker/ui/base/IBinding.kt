package com.absinthe.libchecker.ui.base

import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding

internal sealed interface IBinding<VB : ViewBinding> {
  val binding: VB

  fun inflateBinding(inflater: LayoutInflater): VB
}
