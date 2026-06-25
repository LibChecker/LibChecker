package com.absinthe.libchecker.features.applist.detail.ui

import android.content.DialogInterface
import androidx.core.text.buildSpannedString
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.action.AppPermissionDetail
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.applist.detail.ui.view.PermissionInfoBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

const val EXTRA_ORIG_PERM_NAME = "EXTRA_ORIG_PERM_NAME"

class PermissionDetailDialogFragment : BaseBottomSheetViewDialogFragment<PermissionInfoBottomSheetView>() {

  private val viewModel: DetailViewModel by activityViewModel()
  private val origPermName by lazy {
    arguments?.getString(EXTRA_ORIG_PERM_NAME).orEmpty()
      .substringBefore(" ") // remove maxSdkVersion suffix
  }

  override fun initRootView(): PermissionInfoBottomSheetView = PermissionInfoBottomSheetView(requireContext())

  override fun init() {
    root.apply {
      icon.load(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android)
      title.text = origPermName
      permissionContentView.label.text.text = context.getText(R.string.not_found)
      permissionContentView.description.text.text = context.getText(R.string.not_found)
      permissionContentView.label.updateContentDescription()
      permissionContentView.description.updateContentDescription()
    }

    lifecycleScope.launch {
      renderPermissionDetail(viewModel.getPermissionDetail(origPermName))
    }
  }

  private fun renderPermissionDetail(detail: AppPermissionDetail) = root.apply {
    icon.load(detail.icon ?: com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android)
    title.text = buildSpannedString {
      append(detail.name)
      detail.providerAppName?.let {
        appendLine()
        append(getString(R.string.lib_permission_provided_by_format, it))
      }
    }
    detail.label?.let { permissionContentView.label.text.text = it }
    detail.description?.let { permissionContentView.description.text.text = it }
    permissionContentView.label.updateContentDescription()
    permissionContentView.description.updateContentDescription()
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
    fun newInstance(origPermName: String): PermissionDetailDialogFragment {
      return PermissionDetailDialogFragment().putArguments(EXTRA_ORIG_PERM_NAME to origPermName)
    }

    var isShowing = false
  }
}
