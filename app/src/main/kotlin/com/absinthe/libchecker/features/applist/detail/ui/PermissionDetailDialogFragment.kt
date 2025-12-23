package com.absinthe.libchecker.features.applist.detail.ui

import android.content.DialogInterface
import androidx.core.text.buildSpannedString
import androidx.fragment.app.FragmentManager
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.features.applist.detail.ui.view.PermissionInfoBottomSheetView
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import timber.log.Timber

const val EXTRA_ORIG_PERM_NAME = "EXTRA_ORIG_PERM_NAME"

class PermissionDetailDialogFragment : BaseBottomSheetViewDialogFragment<PermissionInfoBottomSheetView>() {

  private val origPermName by lazy {
    arguments?.getString(EXTRA_ORIG_PERM_NAME).orEmpty()
      .substringBefore(" ") // remove maxSdkVersion suffix
  }

  override fun initRootView(): PermissionInfoBottomSheetView = PermissionInfoBottomSheetView(requireContext())

  override fun init() {
    val pm = SystemServices.packageManager
    root.apply {
      icon.load(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android)
      permissionContentView.label.text.text = context.getText(R.string.not_found)
      permissionContentView.description.text.text = context.getText(R.string.not_found)

      val permissionInfo = runCatching {
        pm.getPermissionInfo(origPermName, 0)
      }.onFailure {
        Timber.e(it)
      }.getOrNull()

      if (permissionInfo == null || permissionInfo.icon == 0) {
        icon.load(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android)
      } else {
        icon.load(permissionInfo.loadIcon(pm))
      }

      val titleText = buildSpannedString {
        append(origPermName)

        val targetPi = runCatching { PackageUtils.getPackageInfo(permissionInfo?.packageName.orEmpty()) }.getOrNull()
        if (permissionInfo != null && targetPi != null) {
          appendLine()
          append(
            String.format(
              getString(R.string.lib_permission_provided_by_format),
              targetPi.getAppName(context.packageManager)
            )
          )
        }
      }
      title.text = titleText

      if (permissionInfo != null) {
        permissionInfo.loadLabel(pm).let { if (it.isNotEmpty()) permissionContentView.label.text.text = it }
        permissionInfo.loadDescription(pm)
          ?.let { if (it.isNotEmpty()) permissionContentView.description.text.text = it }
      }
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
    fun newInstance(origPermName: String): PermissionDetailDialogFragment {
      return PermissionDetailDialogFragment().putArguments(EXTRA_ORIG_PERM_NAME to origPermName)
    }

    var isShowing = false
  }
}
