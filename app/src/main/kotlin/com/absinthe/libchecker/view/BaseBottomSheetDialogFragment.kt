package com.absinthe.libchecker.view

import android.os.Bundle
import android.view.View
import com.absinthe.libchecker.R
import com.absinthe.libraries.utils.utils.UiUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            it.findViewById<View>(com.google.android.material.R.id.container).fitsSystemWindows = false
            UiUtils.setSystemBarStyle(it)
        }
    }

    override fun onStop() {
        super.onStop()
        dialog?.show()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.DialogAnimation
    }

}