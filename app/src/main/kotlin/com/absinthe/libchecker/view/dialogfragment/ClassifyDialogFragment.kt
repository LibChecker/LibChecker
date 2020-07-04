package com.absinthe.libchecker.view.dialogfragment

import android.app.Dialog
import android.os.Bundle
import com.absinthe.libchecker.bean.AppItem
import com.absinthe.libchecker.view.ClassifyDialogView
import com.absinthe.libchecker.view.LCDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

const val EXTRA_ITEM_LIST = "EXTRA_ITEM_LIST"

class ClassifyDialogFragment : LCDialogFragment() {

    var dialogTitle: String = ""
    var item: ArrayList<AppItem> = ArrayList()
    private val dialogView by lazy { ClassifyDialogView(requireContext()) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (savedInstanceState != null) {
            savedInstanceState.getParcelableArrayList<AppItem>(
                EXTRA_ITEM_LIST
            )?.toList()?.let {
                dialogView.adapter.setList(it)
                item = it as ArrayList<AppItem>
            }
        } else {
            dialogView.adapter.setList(item)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(dialogTitle)
            .setView(dialogView)
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(EXTRA_ITEM_LIST, item)
        super.onSaveInstanceState(outState)
    }
}