package com.absinthe.libchecker.features.applist.detail.ui

import android.content.DialogInterface
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.features.applist.detail.ui.view.PermissionInfoBottomSheetView
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import timber.log.Timber

const val EXTRA_ORIG_PERM_NAME = "EXTRA_ORIG_PERM_NAME"

class PermissionDetailDialogFragment : BaseBottomSheetViewDialogFragment<PermissionInfoBottomSheetView>() {

  private val origPermName by lazy {
    arguments?.getString(EXTRA_ORIG_PERM_NAME).orEmpty()
      .substringBefore(" ") // remove maxSdkVersion suffix
  }

  override fun initRootView(): PermissionInfoBottomSheetView =
    PermissionInfoBottomSheetView(requireContext())

  override fun init() {
    root.apply {
      title.text = origPermName
      lifecycleScope.launch {
        icon.load(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android) {
          crossfade(true)
        }
        permissionContentView.description.text.text = context.getText(R.string.not_found)
      }
    }
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun onStart() {
    super.onStart()
    SystemServices.packageManager.let { pm ->
      val pi = runCatching {
        pm.getPermissionInfo(origPermName, 0)
      }.getOrElse {
        Timber.e(it)
        root.showNotFound()
        return
      }
      root.apply {
        pi.loadIcon(pm)?.let { icon.load(it) { crossfade(true) } }
        pi.loadLabel(pm).let { if (it.isNotEmpty()) permissionContentView.label.text.text = it }
        pi.loadDescription(pm)
          ?.let { if (it.isNotEmpty()) permissionContentView.description.text.text = it }
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
    fun newInstance(origPermName: String): PermissionDetailDialogFragment {
      return PermissionDetailDialogFragment().putArguments(EXTRA_ORIG_PERM_NAME to origPermName)
    }

    var isShowing = false
  }
}
