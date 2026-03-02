package com.absinthe.libchecker.utils.extensions

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment

@MainThread
fun <T : Fragment> T.putArguments(bundle: Bundle): T {
  arguments = bundle
  return this
}

@MainThread
fun <T : Fragment> T.putArguments(vararg pairs: Pair<String, Any?>): T = putArguments(bundleOf(*pairs))

fun DialogFragment.isShowing() = this.dialog?.isShowing == true && !this.isRemoving
