package com.absinthe.libchecker.ui.fragment.detail.impl

import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.SIGNATURES
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.recyclerview.diff.LibStringDiffUtil
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.fragment.BaseDetailFragment
import com.absinthe.libchecker.ui.fragment.EXTRA_TYPE
import com.absinthe.libchecker.ui.fragment.detail.LocatedCount
import com.absinthe.libchecker.ui.fragment.detail.SignatureDetailBSDFragment
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import rikka.core.util.ClipboardUtils

class SignaturesAnalysisFragment : BaseDetailFragment<FragmentLibComponentBinding>() {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = false

  override fun init() {
    binding.list.adapter = adapter

    viewModel.signaturesLibItems.observe(viewLifecycleOwner) {
      if (it.isEmpty()) {
        emptyView.text.text = getString(R.string.uncharted_territory)
      } else {
        if (viewModel.queriedText?.isNotEmpty() == true) {
          filterList(viewModel.queriedText!!)
        } else {
          context?.let {
            binding.list.addItemDecoration(dividerItemDecoration)
          }
          adapter.setDiffNewData(it.toMutableList(), afterListReadyTask)
        }
      }

      if (!isListReady) {
        viewModel.itemsCountLiveData.value = LocatedCount(locate = type, count = it.size)
        viewModel.itemsCountList[type] = it.size
        isListReady = true
      }
    }

    adapter.apply {
      animationEnable = true
      setOnItemClickListener { _, view, position ->
        if (AntiShakeUtils.isInvalidClick(view)) {
          return@setOnItemClickListener
        }
        openSignatureDetailDialog(position)
      }
      setOnItemLongClickListener { _, _, position ->
        ClipboardUtils.put(requireContext(), getItem(position).item.source)
        VersionCompat.showCopiedOnClipboardToast(context)
        true
      }
      setDiffCallback(LibStringDiffUtil())
      setEmptyView(emptyView)
    }
    viewModel.initSignatures()
  }

  override fun getFilterListByText(text: String): List<LibStringItemChip>? {
    return viewModel.signaturesLibItems.value?.filter { it.item.name.contains(text, true) }
  }

  private fun openSignatureDetailDialog(position: Int) {
    val source = adapter.getItem(position).item.source
    SignatureDetailBSDFragment.newInstance(source.orEmpty())
      .show(childFragmentManager, SignatureDetailBSDFragment::class.java.name)
  }

  companion object {
    fun newInstance(packageName: String): SignaturesAnalysisFragment {
      return SignaturesAnalysisFragment().putArguments(
        EXTRA_PACKAGE_NAME to packageName,
        EXTRA_TYPE to SIGNATURES,
      )
    }
  }
}
