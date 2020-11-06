package com.absinthe.libchecker.ui.fragment.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.databinding.LayoutBottomSheetHeaderBinding
import com.absinthe.libchecker.view.ClassifyDialogView
import com.absinthe.libraries.utils.utils.UiUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

const val EXTRA_TITLE = "EXTRA_TITLE"
const val EXTRA_ITEM_LIST = "EXTRA_ITEM_LIST"

class ClassifyBottomSheetDialogFragment : BottomSheetDialogFragment() {

    var item: ArrayList<LCItem> = ArrayList()
    private val dialogView by lazy { ClassifyDialogView(requireContext()) }
    private val dialogTitle by lazy { arguments?.getString(EXTRA_TITLE) ?: "" }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (savedInstanceState != null) {
            savedInstanceState.getParcelableArrayList<LCItem>(
                EXTRA_ITEM_LIST
            )?.toList()?.let {
                dialogView.adapter.setList(it)
                item = it as ArrayList<LCItem>
            }
        } else {
            dialogView.adapter.setList(item)
        }
        val header = LayoutBottomSheetHeaderBinding.inflate(layoutInflater)
        header.tvTitle.text = dialogTitle
        dialogView.adapter.setHeaderView(header.root)
        return dialogView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(EXTRA_ITEM_LIST, item)
        outState.putString(EXTRA_TITLE, dialogTitle)
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            it.findViewById<View>(com.google.android.material.R.id.container).fitsSystemWindows = false
            UiUtils.setSystemBarStyle(it)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.DialogAnimation
    }
}