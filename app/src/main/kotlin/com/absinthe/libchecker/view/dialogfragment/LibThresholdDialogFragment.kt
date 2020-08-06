package com.absinthe.libchecker.view.dialogfragment

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.view.settings.LibReferenceThresholdView

class LibThresholdDialogFragment : DialogFragment() {

    private val dialogView by lazy {
        LibReferenceThresholdView(
            requireContext()
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
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