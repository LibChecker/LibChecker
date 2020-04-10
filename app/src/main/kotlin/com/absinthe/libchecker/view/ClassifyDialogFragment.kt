package com.absinthe.libchecker.view

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.absinthe.libchecker.R
import com.absinthe.libchecker.viewholder.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ClassifyDialogFragment : DialogFragment() {

    lateinit var dialogView: ClassifyDialogView
    var item: List<AppItem> = ArrayList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView = ClassifyDialogView(requireContext())
        dialogView.adapter.items = item

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(String.format(getString(R.string.title_classify_dialog), getTitle()))
            .setView(dialogView)
            .create()
    }

    private fun getTitle(): String {
        if (item.isEmpty()) {
            return ""
        }

        return when (item[0].abi) {
            ARMV8 -> getString(R.string.string_64_bit)
            ARMV7 -> getString(R.string.string_32_bit)
            ARMV5 -> getString(R.string.string_32_bit)
            NO_LIBS -> getString(R.string.no_libs)
            else -> ""
        }
    }
}