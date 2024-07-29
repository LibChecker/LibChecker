package com.absinthe.libchecker.utils.extensions

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.features.applist.detail.bean.DetailExtraBean
import com.absinthe.libchecker.features.applist.detail.ui.AppDetailActivity
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_DETAIL_BEAN
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_LC_ITEM
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.features.applist.detail.ui.OverlayDetailBottomSheetDialogFragment
import com.absinthe.libchecker.features.statistics.ui.EXTRA_REF_LABEL
import com.absinthe.libchecker.features.statistics.ui.EXTRA_REF_LIST
import com.absinthe.libchecker.features.statistics.ui.EXTRA_REF_NAME
import com.absinthe.libchecker.features.statistics.ui.EXTRA_REF_TYPE
import com.absinthe.libchecker.features.statistics.ui.LibReferenceActivity

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

fun Activity.launchLibReferencePage(
  refName: String,
  refLabel: String?,
  refType: Int,
  refList: Array<String>?
) {
  val intent = Intent(this, LibReferenceActivity::class.java)
    .putExtras(
      bundleOf(
        EXTRA_REF_NAME to refName,
        EXTRA_REF_LABEL to refLabel,
        EXTRA_REF_TYPE to refType,
        EXTRA_REF_LIST to refList
      )
    )
  startActivity(intent)
}

fun Activity.isKeyboardShowing(): Boolean {
  val imeInsets = ViewCompat.getRootWindowInsets(window.decorView.rootView)
    ?.getInsets(WindowInsetsCompat.Type.ime())
  return (imeInsets?.bottom ?: 0) > 0
}
