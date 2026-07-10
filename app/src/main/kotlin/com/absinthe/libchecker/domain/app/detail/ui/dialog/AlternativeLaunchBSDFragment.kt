package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AlternativeLaunchAction
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.view.AlternativeLaunchBSDView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class AlternativeLaunchBSDFragment : BaseBottomSheetViewDialogFragment<AlternativeLaunchBSDView>() {

  private val viewModel: DetailViewModel by activityViewModel()
  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }

  override fun initRootView(): AlternativeLaunchBSDView {
    return AlternativeLaunchBSDView(requireContext())
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    lifecycleScope.launch {
      val packageName = packageName ?: run {
        activity?.showToast(R.string.toast_cant_open_app)
        dismiss()
        return@launch
      }
      val list = viewModel.getAlternativeLaunchItems(packageName)
      if (list.isEmpty()) {
        activity?.showToast(R.string.toast_cant_open_app)
        dismiss()
        return@launch
      }
      root.bind(list, ::handleAction)
    }
  }

  private fun handleAction(action: AlternativeLaunchAction) {
    when (action) {
      is AlternativeLaunchAction.OpenActivity -> {
        val packageName = packageName ?: return
        runCatching {
          startActivity(
            Intent().apply {
              setPackage(packageName)
              component = ComponentName(packageName, action.item.className)
            }
          )
        }.onFailure {
          activity?.showToast(R.string.toast_cant_open_app)
        }
      }
    }
  }
}
