package com.absinthe.libchecker.utils.extensions

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.model.DetailExtraBean
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_DETAIL_BEAN
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_LC_ITEM
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.ui.AppDetailActivity
import com.absinthe.libchecker.domain.app.detail.ui.dialog.OverlayDetailBottomSheetDialogFragment
import com.absinthe.libchecker.domain.statistics.reference.ui.EXTRA_REF_LABEL
import com.absinthe.libchecker.domain.statistics.reference.ui.EXTRA_REF_LIST
import com.absinthe.libchecker.domain.statistics.reference.ui.EXTRA_REF_NAME
import com.absinthe.libchecker.domain.statistics.reference.ui.EXTRA_REF_TYPE
import com.absinthe.libchecker.domain.statistics.reference.ui.LibReferenceActivity
import com.absinthe.libchecker.utils.Toasty

fun FragmentActivity.launchDetailPage(item: LCItem, refName: String? = null, refType: Int = NATIVE, forceDetail: Boolean = false) {
  if (item.isArchived) {
    Toasty.showLong(this, R.string.toast_archived_app_detail_unavailable)
    return
  }
  findViewById<View>(androidx.appcompat.R.id.search_src_text)?.clearFocus()
  if (item.abi.toInt() == Constants.OVERLAY && !forceDetail) {
    OverlayDetailBottomSheetDialogFragment().apply {
      arguments = Bundle().apply {
        putParcelable(EXTRA_LC_ITEM, item)
      }
      show(
        supportFragmentManager,
        OverlayDetailBottomSheetDialogFragment::class.java.name
      )
    }
  } else {
    val intent = Intent(this, AppDetailActivity::class.java)
      .putExtras(
        Bundle().apply {
          putString(EXTRA_PACKAGE_NAME, item.packageName)
          putString(EXTRA_REF_NAME, refName)
          putInt(EXTRA_REF_TYPE, refType)
          putParcelable(
            EXTRA_DETAIL_BEAN,
            DetailExtraBean(
              item.features,
              item.variant
            )
          )
        }
      )
    runCatching {
      startActivity(intent)
    }.onFailure {
      Toasty.showLong(this, "But why…")
    }
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
      Bundle().apply {
        putString(EXTRA_REF_NAME, refName)
        putString(EXTRA_REF_LABEL, refLabel)
        putInt(EXTRA_REF_TYPE, refType)
        putStringArray(EXTRA_REF_LIST, refList)
      }
    )
  startActivity(intent)
}

fun Activity.isKeyboardShowing(): Boolean {
  val imeInsets = ViewCompat.getRootWindowInsets(window.decorView.rootView)
    ?.getInsets(WindowInsetsCompat.Type.ime())
  return (imeInsets?.bottom ?: 0) > 0
}
