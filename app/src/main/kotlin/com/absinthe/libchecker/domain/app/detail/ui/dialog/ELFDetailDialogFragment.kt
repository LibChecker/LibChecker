package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.content.DialogInterface
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.domain.app.detail.action.AppElfDetail
import com.absinthe.libchecker.domain.app.detail.model.ElfDetailBottomSheetState
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.ElfDetailResult
import com.absinthe.libchecker.domain.app.detail.ui.view.ELFInfoBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber

const val EXTRA_ELF_PATH = "EXTRA_ELF_PATH"
const val EXTRA_RULE_ICON = "EXTRA_RULE_ICON"

class ELFDetailDialogFragment : BaseBottomSheetViewDialogFragment<ELFInfoBottomSheetView>() {

  private val viewModel: DetailViewModel by activityViewModel()
  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME).orEmpty() }
  private val elfPath by lazy { arguments?.getString(EXTRA_ELF_PATH).orEmpty() }
  private val elfTitle by lazy { elfPath.substringAfterLast('/') }
  private val ruleIcon by lazy {
    arguments?.getInt(EXTRA_RULE_ICON) ?: com.absinthe.lc.rulesbundle.R.drawable.ic_sdk_placeholder
  }

  override fun initRootView(): ELFInfoBottomSheetView = ELFInfoBottomSheetView(requireContext())

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    root.bind(
      ElfDetailBottomSheetState.Loading(
        title = elfTitle,
        iconRes = ruleIcon
      )
    )
    collectElfDetailResults()
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun onStart() {
    super.onStart()
    viewModel.loadElfDetail(packageName, elfPath)
  }

  private fun collectElfDetailResults() {
    lifecycleScope.launch {
      viewModel.elfDetailResults.collect(::handleElfDetailResult)
    }
  }

  private fun handleElfDetailResult(loadResult: ElfDetailResult) {
    if (loadResult.packageName != packageName || loadResult.elfPath != elfPath) {
      return
    }
    loadResult.result.onFailure {
      Timber.e(it, "Failed to load ELF detail: $elfPath")
    }.getOrNull()?.let(::renderElfDetail)
  }

  private fun renderElfDetail(info: AppElfDetail) {
    root.bind(
      ElfDetailBottomSheetState.Content(
        title = elfTitle,
        iconRes = ruleIcon,
        dependenciesText = info.deps.joinToString(", "),
        entryPointsText = info.entryPoints.joinToString(System.lineSeparator()) {
          "◉${Typography.nbsp}$it"
        },
        isStripped = info.isStripped
      )
    )
  }

  override fun show(manager: FragmentManager, tag: String?) {
    if (!isShowing) {
      isShowing = true
      super.show(manager, tag)
    }
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    isShowing = false
  }

  companion object {
    fun newInstance(packageName: String, elfPath: String, ruleIcon: Int): ELFDetailDialogFragment {
      return ELFDetailDialogFragment().putArguments(
        EXTRA_PACKAGE_NAME to packageName,
        EXTRA_ELF_PATH to elfPath,
        EXTRA_RULE_ICON to ruleIcon
      )
    }

    var isShowing = false
  }
}
