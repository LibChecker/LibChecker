package com.absinthe.libchecker.view.dialogfragment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import com.absinthe.libchecker.bean.AppItem
import com.absinthe.libchecker.view.ClassifyDialogView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import rikka.material.app.MaterialDialogFragment

const val EXTRA_TITLE = "EXTRA_TITLE"
const val EXTRA_ITEM_LIST = "EXTRA_ITEM_LIST"

class ClassifyDialogFragment : MaterialDialogFragment() {

    var item: ArrayList<AppItem> = ArrayList()
    private val dialogView by lazy { ClassifyDialogView(requireContext()) }
    private val dialogTitle by lazy { arguments?.getString(EXTRA_TITLE) ?: "" }

    override fun onCreateDialog(context: Context, savedInstanceState: Bundle?): Dialog {
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setTitle(dialogTitle)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(EXTRA_ITEM_LIST, item)
        outState.putString(EXTRA_TITLE, dialogTitle)
        super.onSaveInstanceState(outState)
    }

    companion object {
        fun newInstance(title: String): ClassifyDialogFragment {
            return ClassifyDialogFragment()
                .apply {
                    arguments = Bundle().apply {
                        putString(EXTRA_TITLE, title)
                    }
                }
        }
    }
}