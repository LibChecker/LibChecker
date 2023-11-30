package com.absinthe.libchecker.features.settings.ui

import android.os.Bundle
import android.view.View
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.features.settings.bean.GetUpdatesItem
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class GetUpdatesDialogFragment : BaseBottomSheetViewDialogFragment<GetUpdatesDialogView>() {

  override fun initRootView(): GetUpdatesDialogView = GetUpdatesDialogView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.addPaddingTop(16.dp)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val items = listOf(
      GetUpdatesItem(
        "GitHub",
        URLManager.GITHUB_REPO_PAGE,
        R.drawable.ic_github
      ),
      GetUpdatesItem(
        "Google Play",
        URLManager.PLAY_STORE_DETAIL_PAGE,
        com.absinthe.lc.rulesbundle.R.drawable.ic_lib_google
      ),
      GetUpdatesItem(
        "Telegram",
        URLManager.TELEGRAM_RELEASES,
        R.drawable.ic_telegram
      ),
      GetUpdatesItem(
        "F-Droid",
        URLManager.FDROID_PAGE,
        R.drawable.ic_fdroid
      )
    )
    root.setItems(items)
  }
}
