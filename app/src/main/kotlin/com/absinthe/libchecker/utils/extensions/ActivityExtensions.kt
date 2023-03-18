package com.absinthe.libchecker.utils.extensions

import android.content.Intent
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.bean.DetailExtraBean
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.ui.detail.AppDetailActivity
import com.absinthe.libchecker.ui.detail.EXTRA_DETAIL_BEAN
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.fragment.detail.EXTRA_LC_ITEM
import com.absinthe.libchecker.ui.fragment.detail.OverlayDetailBottomSheetDialogFragment
import com.absinthe.libchecker.ui.main.EXTRA_REF_NAME
import com.absinthe.libchecker.ui.main.EXTRA_REF_TYPE

fun FragmentActivity.launchDetailPage(item: LCItem, refName: String? = null, refType: Int = NATIVE) {
  findViewById<View>(androidx.appcompat.R.id.search_src_text)?.clearFocus()
  if (item.abi.toInt() == Constants.OVERLAY) {
    OverlayDetailBottomSheetDialogFragment().apply {
      arguments = bundleOf(
        EXTRA_LC_ITEM to item
      )
      show(
        supportFragmentManager,
        OverlayDetailBottomSheetDialogFragment::class.java.name
      )
    }
  } else {
    val intent = Intent(this, AppDetailActivity::class.java)
      .putExtras(
        bundleOf(
          EXTRA_PACKAGE_NAME to item.packageName,
          EXTRA_REF_NAME to refName,
          EXTRA_REF_TYPE to refType,
          EXTRA_DETAIL_BEAN to DetailExtraBean(
            item.features,
            item.variant
          )
        )
      )
    startActivity(intent)
  }
}
