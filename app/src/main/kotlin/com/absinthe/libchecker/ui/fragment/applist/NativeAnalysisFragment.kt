package com.absinthe.libchecker.ui.fragment.applist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.constant.*
import com.absinthe.libchecker.constant.librarymap.BaseMap
import com.absinthe.libchecker.constant.librarymap.NativeLibMap
import com.absinthe.libchecker.databinding.FragmentLibNativeBinding
import com.absinthe.libchecker.databinding.LayoutEmptyListBinding
import com.absinthe.libchecker.extensions.addPaddingBottom
import com.absinthe.libchecker.recyclerview.adapter.LibStringAdapter
import com.absinthe.libchecker.recyclerview.diff.LibStringDiffUtil
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.fragment.BaseFragment
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.view.dialogfragment.LibDetailDialogFragment
import com.absinthe.libchecker.viewmodel.DetailViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.libraries.utils.utils.UiUtils
import rikka.core.util.ClipboardUtils
import java.lang.ref.WeakReference

const val MODE_SORT_BY_SIZE = 0
const val MODE_SORT_BY_LIB = 1

class NativeAnalysisFragment : BaseFragment<FragmentLibNativeBinding>(R.layout.fragment_lib_native), Sortable {

    private val viewModel by activityViewModels<DetailViewModel>()
    private val emptyLayoutBinding by lazy { LayoutEmptyListBinding.inflate(layoutInflater) }
    private val type by lazy { arguments?.getInt(EXTRA_TYPE) ?: NATIVE }
    private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) ?: "" }
    private val adapter by lazy { LibStringAdapter(arguments?.getInt(EXTRA_TYPE) ?: NATIVE) }

    override fun initBinding(view: View): FragmentLibNativeBinding = FragmentLibNativeBinding.bind(view)

    override fun init() {
        binding.apply {
            list.apply {
                adapter = this@NativeAnalysisFragment.adapter
                addPaddingBottom(UiUtils.getNavBarHeight(requireActivity().contentResolver))
            }
        }

        viewModel.apply {
            val observer = Observer<List<LibStringItemChip>> {
                if (it.isEmpty()) {
                    emptyLayoutBinding.text.text = getString(R.string.empty_list)
                } else {
                    adapter.setDiffNewData(it.toMutableList())
                }
            }

            if (type == DEX) {
                dexLibItems.observe(viewLifecycleOwner, observer)
            } else {
                nativeLibItems.observe(viewLifecycleOwner, observer)
            }
        }

        fun openLibDetailDialog(position: Int) {
            val name = adapter.getItem(position).item.name
            val regexName = NativeLibMap.findRegex(name)?.regexName
            LibDetailDialogFragment.newInstance(name, adapter.type, regexName).show(childFragmentManager, tag)
        }

        adapter.apply {
            setOnItemClickListener { _, view, position ->
                if (AntiShakeUtils.isInvalidClick(view)) {
                    return@setOnItemClickListener
                }
                openLibDetailDialog(position)
            }
            setOnItemLongClickListener { _, _, position ->
                ClipboardUtils.put(requireContext(), getItem(position).item.name)
                Toasty.show(requireContext(), R.string.toast_copied_to_clipboard)
                true
            }
            setOnItemChildClickListener { _, view, position ->
                if (AntiShakeUtils.isInvalidClick(view)) {
                    return@setOnItemChildClickListener
                }
                openLibDetailDialog(position)
            }
            setDiffCallback(LibStringDiffUtil())
            emptyLayoutBinding.text.text = getString(R.string.loading)
            setEmptyView(emptyLayoutBinding.root)
        }

        if (type == DEX) {
            viewModel.initDexData(packageName)
        } else {
            viewModel.initSoAnalysisData(packageName)
        }
    }

    override fun onResume() {
        super.onResume()
        Sortable.currentReference = WeakReference(this)
    }

    override fun sort() {
        viewModel.sortMode = if (viewModel.sortMode == MODE_SORT_BY_SIZE) {
            val map = BaseMap.getMap(adapter.type)
            adapter.setDiffNewData(adapter.data.sortedByDescending { map.contains(it.item.name) }
                .toMutableList())
            MODE_SORT_BY_LIB
        } else {
            adapter.setDiffNewData(adapter.data.sortedByDescending { it.item.size }
                .toMutableList())
            MODE_SORT_BY_SIZE
        }
        GlobalValues.libSortMode.value = viewModel.sortMode
        SPUtils.putInt(Constants.PREF_LIB_SORT_MODE, viewModel.sortMode)
    }

    companion object {
        fun newInstance(packageName: String, @LibType type: Int): NativeAnalysisFragment {
            return NativeAnalysisFragment()
                .apply {
                    arguments = Bundle().apply {
                        putString(EXTRA_PACKAGE_NAME, packageName)
                        putInt(EXTRA_TYPE, type)
                    }
                }
        }
    }
}
