package com.absinthe.libchecker.features.about

import android.os.Bundle
import android.view.View
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class DevelopersDialogFragment : BaseBottomSheetViewDialogFragment<DevelopersDialogView>() {

  override fun initRootView(): DevelopersDialogView = DevelopersDialogView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.addPaddingTop(16.dp)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val items = listOf(
      DeveloperInfo(
        "Absinthe",
        "Developer & Designer",
        "https://github.com/zhaobozhen",
        R.drawable.pic_rabbit
      ),
      DeveloperInfo(
        "Goooler",
        "Code Tidy & Optimize",
        "https://github.com/Goooler",
        R.drawable.pic_kali
      ),
      DeveloperInfo(
        "qhy040404",
        "Developer",
        "https://github.com/qhy040404",
        R.drawable.pic_qhy040404
      ),
      DeveloperInfo(
        "Source Code",
        URLManager.GITHUB_REPO_PAGE,
        URLManager.GITHUB_REPO_PAGE,
        R.drawable.ic_github
      )
    )

    root.setItems(items)
  }
}
