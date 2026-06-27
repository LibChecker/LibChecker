package com.absinthe.libchecker.features.applist.detail.ui

import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.detail.ui.EXTRA_ENTITY
import com.absinthe.libchecker.domain.snapshot.detail.ui.SnapshotDetailActivity
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.UiUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetailPackageComparisonController(
  private val activity: FragmentActivity,
  private val viewModel: DetailViewModel,
  private val toolbarController: DetailToolbarController,
  private val coroutineScope: CoroutineScope,
  private val currentUiGeneration: () -> Int
) {

  fun setupIfAvailable(packageName: String, uiGeneration: Int) {
    coroutineScope.launch {
      if (!viewModel.isInstalledAppComparisonAvailable(packageName)) {
        return@launch
      }
      if (uiGeneration != currentUiGeneration()) {
        return@launch
      }
      toolbarController.addCompareAction {
        compareWithInstalledPackage(packageName)
      }
    }
  }

  private fun compareWithInstalledPackage(packageName: String) {
    coroutineScope.launch {
      val basePackage = viewModel.loadInstalledAppComparisonPackage(packageName)
      if (basePackage == null) {
        Toasty.showLong(activity, R.string.toast_cant_open_app)
        return@launch
      }
      navigateToSnapshotDetailPage(basePackage, viewModel.packageInfo)
    }
  }

  private fun navigateToSnapshotDetailPage(basePackage: PackageInfo, analysisPackage: PackageInfo) {
    coroutineScope.launch(Dispatchers.Main) {
      val dialog = UiUtils.createLoadingDialog(activity)
      dialog.show()
      val diff = viewModel.buildPackageComparisonSnapshotItem(basePackage, analysisPackage)
      dialog.dismiss()

      val intent = Intent(activity, SnapshotDetailActivity::class.java)
        .putExtras(
          Bundle().apply {
            putSerializable(EXTRA_ENTITY, diff)
          }
        )
      activity.startActivity(intent)
    }
  }
}
