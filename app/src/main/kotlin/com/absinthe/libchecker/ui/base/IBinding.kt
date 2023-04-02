package com.absinthe.libchecker.ui.base

import androidx.viewbinding.ViewBinding

internal sealed interface IBinding<VB : ViewBinding> {
  val binding: VB
}
