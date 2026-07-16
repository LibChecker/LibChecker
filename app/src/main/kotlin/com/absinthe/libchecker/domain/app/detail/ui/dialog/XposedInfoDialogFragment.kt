package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.content.DialogInterface
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.feature.xposedFeatureDialogSpec
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoAction
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.FeaturesDialog
import com.absinthe.libchecker.domain.app.detail.ui.view.XposedInfoBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class XposedInfoDialogFragment : BaseBottomSheetViewDialogFragment<XposedInfoBottomSheetView>() {

  private val viewModel: DetailViewModel by activityViewModel()
  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME).orEmpty() }

  override fun initRootView(): XposedInfoBottomSheetView {
    return XposedInfoBottomSheetView(requireContext())
  }

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    lifecycleScope.launch {
      val display = viewModel.getXposedInfoBottomSheetDisplay(packageName)
      val context = context ?: return@launch
      if (display == null) {
        dismiss()
        FeaturesDialog.show(context, xposedFeatureDialogSpec())
        return@launch
      }
      root.bind(display, ::handleAction)
    }
  }

  private fun handleAction(action: XposedInfoAction) {
    when (action) {
      is XposedInfoAction.OpenSettings -> openXposedSettings(action)
    }
  }

  private fun openXposedSettings(action: XposedInfoAction.OpenSettings) {
    val activity = activity ?: return
    val settingsIntent = action.intent ?: run {
      Toasty.showShort(activity, R.string.toast_cant_open_app)
      return
    }
    runCatching {
      activity.startActivity(settingsIntent)
    }.onFailure {
      Toasty.showShort(activity, it.message.toString())
    }
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

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
    fun newInstance(packageName: String): XposedInfoDialogFragment {
      return XposedInfoDialogFragment().putArguments(EXTRA_PACKAGE_NAME to packageName)
    }

    var isShowing = false
  }
}
