package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.provider

import com.absinthe.libchecker.domain.app.detail.ui.dialog.LibDetailDialogFragment
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotDetailLibraryDialogTarget
import com.absinthe.libchecker.ui.base.BaseActivity
import com.chad.library.adapter.base.provider.BaseNodeProvider

fun BaseNodeProvider.showSnapshotDetailLibraryDialog(target: SnapshotDetailLibraryDialogTarget) {
  val fragmentManager = (context as BaseActivity<*>).supportFragmentManager
  LibDetailDialogFragment.newInstance(
    libName = target.name,
    type = target.type,
    regexName = target.regexName,
    enableLibraryInsight = false
  )
    .show(fragmentManager, LibDetailDialogFragment::class.java.name)
}
