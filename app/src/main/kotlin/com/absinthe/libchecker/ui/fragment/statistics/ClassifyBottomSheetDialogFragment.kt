package com.absinthe.libchecker.ui.fragment.statistics

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.extensions.dp
import com.absinthe.libchecker.ui.fragment.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.statistics.ClassifyDialogView

const val EXTRA_TITLE = "EXTRA_TITLE"
const val EXTRA_ITEM_LIST = "EXTRA_ITEM_LIST"

class ClassifyBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<ClassifyDialogView>() {

    var item: ArrayList<LCItem> = ArrayList()
    private val dialogTitle by lazy { arguments?.getString(EXTRA_TITLE) ?: "" }
    private var mListener: OnDismissListener? = null
    private lateinit var headerView: BottomSheetHeaderView

    override fun initRootView(): ClassifyDialogView = ClassifyDialogView(requireContext(), lifecycleScope)
    override fun getHeaderView(): BottomSheetHeaderView = headerView

    override fun init() {
        headerView = BottomSheetHeaderView(requireContext()).apply {
            layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.topMargin = 8.dp
                it.bottomMargin = 24.dp
            }
            title.text = dialogTitle
        }
        root.adapter.setHeaderView(headerView)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        if (savedInstanceState != null) {
            savedInstanceState.getParcelableArrayList<LCItem>(
                EXTRA_ITEM_LIST
            )?.toList()?.let {
                root.adapter.setList(it)
                item = ArrayList(it)
            }
        } else {
            root.adapter.setList(item)
        }

        return view
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