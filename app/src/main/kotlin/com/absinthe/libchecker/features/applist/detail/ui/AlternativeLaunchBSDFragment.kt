package com.absinthe.libchecker.features.applist.detail.ui

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.features.applist.detail.bean.AlternativeLaunchItem
import com.absinthe.libchecker.features.applist.detail.ui.view.AlternativeLaunchBSDView
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnItemClickListener

class AlternativeLaunchBSDFragment : BaseBottomSheetViewDialogFragment<AlternativeLaunchBSDView>() {

  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }

  override fun initRootView(): AlternativeLaunchBSDView =
    AlternativeLaunchBSDView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    packageName?.let { packageName ->
      val packageInfo = runCatching {
        PackageUtils.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
      }.getOrNull()
      val list = if (packageInfo?.activities == null) {
        emptyList()
      } else {
        packageInfo.activities.asSequence()
          .filter { it.exported }
          .map {
            AlternativeLaunchItem(
              it.loadLabel(SystemServices.packageManager).toString(),
              it.name
            )
          }
          .toList()
      }
      if (list.isEmpty()) {
        activity?.showToast(R.string.toast_cant_open_app)
        dismiss()
        return
      }
      root.adapter.setList(list)
      root.adapter.setOnItemClickListener(object : OnItemClickListener {
        override fun onItemClick(adapter: BaseQuickAdapter<*, *>, view: View, position: Int) {
          val className = root.adapter.data[position].className
          runCatching {
            startActivity(
              Intent().also {
                it.setPackage(packageName)
                it.component = ComponentName(packageName, className)
              }
            )
          }.onFailure {
            activity?.showToast(R.string.toast_cant_open_app)
          }
        }
      })
    }
  }
}
