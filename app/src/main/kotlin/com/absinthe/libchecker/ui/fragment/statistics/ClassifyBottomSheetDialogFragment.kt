package com.absinthe.libchecker.ui.fragment.statistics

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.databinding.LayoutBottomSheetHeaderBinding
import com.absinthe.libchecker.ui.fragment.BaseBottomSheetNoBindingDialogFragment
import com.absinthe.libchecker.view.statistics.ClassifyDialogView

const val EXTRA_TITLE = "EXTRA_TITLE"
const val EXTRA_ITEM_LIST = "EXTRA_ITEM_LIST"

class ClassifyBottomSheetDialogFragment : BaseBottomSheetNoBindingDialogFragment() {

    var item: ArrayList<LCItem> = ArrayList()
    private val dialogView by lazy { ClassifyDialogView(requireContext(), lifecycleScope) }
    private val dialogTitle by lazy { arguments?.getString(EXTRA_TITLE) ?: "" }
    private var mListener: OnDismissListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        mListener?.onDismiss()
        mListener = null
    }

    fun setOnDismissListener(listener: OnDismissListener) {
        mListener = listener
    }

    interface OnDismissListener {
        fun onDismiss()
    }
}