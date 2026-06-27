package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.content.DialogInterface
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.ui.view.ELFInfoBottomSheetView
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

const val EXTRA_ELF_PATH = "EXTRA_ELF_PATH"
const val EXTRA_RULE_ICON = "EXTRA_RULE_ICON"

class ELFDetailDialogFragment : BaseBottomSheetViewDialogFragment<ELFInfoBottomSheetView>() {

  private val viewModel: DetailViewModel by activityViewModel()
  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME).orEmpty() }
  private val elfPath by lazy { arguments?.getString(EXTRA_ELF_PATH).orEmpty() }
  private val ruleIcon by lazy {
    arguments?.getInt(EXTRA_RULE_ICON) ?: com.absinthe.lc.rulesbundle.R.drawable.ic_sdk_placeholder
  }

  override fun initRootView(): ELFInfoBottomSheetView = ELFInfoBottomSheetView(requireContext())

  override fun init() {
    root.apply {
      maxPeekHeightPercentage = 0.67f
      title.text = elfPath.split("/").last()
      lifecycleScope.launch {
        icon.load(ruleIcon) {
          crossfade(true)
        }
        setContent(getString(R.string.loading), getString(R.string.loading), false)
      }
    }
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun onStart() {
    super.onStart()
    lifecycleScope.launch {
      val info = viewModel.getElfDetail(packageName, elfPath) ?: return@launch
      root.apply {
        setContent(
          info.deps.joinToString(", "),
          info.entryPoints.joinToString(System.lineSeparator()) { "◉${Typography.nbsp}$it" },
          info.isStripped
        )
      }
    }
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
