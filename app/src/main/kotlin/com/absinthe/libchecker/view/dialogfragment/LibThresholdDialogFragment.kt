package com.absinthe.libchecker.view.dialogfragment

import android.app.Dialog
import android.os.Bundle
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.view.LCDialogFragment
import com.absinthe.libchecker.view.settings.LibReferenceThresholdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LibThresholdDialogFragment : LCDialogFragment() {

    private val dialogView by lazy {
        LibReferenceThresholdView(
            requireContext()
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setTitle(R.string.lib_ref_threshold)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                GlobalValues.libReferenceThreshold.value = dialogView.slider.value.toInt()
                SPUtils.putInt(Constants.PREF_LIB_REF_THRESHOLD, dialogView.slider.value.toInt())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }
}