package com.absinthe.libchecker.features.applist.detail.ui

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import androidx.fragment.app.FragmentManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.features.applist.detail.FeaturesDialog
import com.absinthe.libchecker.features.applist.detail.ui.adapter.node.XposedDetailItem
import com.absinthe.libchecker.features.applist.detail.ui.view.XposedInfoBottomSheetView
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

const val CATEGORY_XPOSED_SETTINGS = "de.robv.android.xposed.category.MODULE_SETTINGS"

class XposedInfoDialogFragment : BaseBottomSheetViewDialogFragment<XposedInfoBottomSheetView>() {

  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME).orEmpty() }

  override fun initRootView(): XposedInfoBottomSheetView = XposedInfoBottomSheetView(requireContext())

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    val pi = runCatching {
      PackageUtils.getPackageInfo(packageName, PackageManager.GET_META_DATA)
    }.getOrNull() ?: run {
      dismiss()
      FeaturesDialog.showXPosedDialog(requireContext())
      return
    }
    root.apply {
      setting.setText(pi.getAppName().orEmpty())
      setting.setOnClickListener {
        val intent = Intent(Intent.ACTION_MAIN).also {
          it.`package` = packageName
          it.addCategory(CATEGORY_XPOSED_SETTINGS)
        }
        val ris = PackageManagerCompat.queryIntentActivities(intent, 0)
        activity?.let {
          if (ris.isEmpty()) {
            Toasty.showShort(it, R.string.toast_cant_open_app)
            return@setOnClickListener
          }

          val launchIntent = Intent(intent).also { i ->
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            i.setClassName(ris[0].activityInfo.packageName, ris[0].activityInfo.name)
          }

          runCatching {
            it.startActivity(launchIntent)
          }.onFailure { t ->
            Toasty.showShort(it, t.message.toString())
          }
        }
      }
      val metadataBundle = PackageUtils.getMetaDataItems(pi).associateBy { it.name }
      val list = mutableListOf<XposedDetailItem>()
      list.add(
        XposedDetailItem(
          iconRes = R.drawable.ic_app_prop,
          tip = context.getString(R.string.lib_detail_xposed_min_version),
          text = metadataBundle["xposedminversion"]?.source.toString(),
          textStyleRes = context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2)
        )
      )
      metadataBundle["xposedscope"]?.let { scopeItem ->
        val appRes = SystemServices.packageManager.getResourcesForApplication(packageName)
        runCatching {
          appRes.getStringArray(scopeItem.size.toInt()).contentToString()
        }.getOrNull()?.let { content ->
          list.add(
            XposedDetailItem(
              iconRes = R.drawable.ic_app_prop,
              tip = context.getString(R.string.lib_detail_xposed_default_scope),
              text = content,
              textStyleRes = context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2)
            )
          )
        }
      }
      ZipFileCompat(pi.applicationInfo!!.sourceDir).use { zipFile ->
        zipFile.getEntry("assets/xposed_init")?.let { entry ->
          val cls = zipFile.getInputStream(entry).bufferedReader().readLines().firstOrNull()
          list.add(
            XposedDetailItem(
              iconRes = R.drawable.ic_app_prop,
              tip = context.getString(R.string.lib_detail_xposed_init_class),
              text = cls.toString(),
              textStyleRes = context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2)
            )
          )
        }
      }
      list.add(
        XposedDetailItem(
          iconRes = R.drawable.ic_content,
          tip = context.getString(R.string.lib_detail_description_tip),
          text = metadataBundle["xposeddescription"]?.source.toString(),
          textStyleRes = context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBody2)
        )
      )
      contentAdapter.setList(list)
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
    fun newInstance(packageName: String): XposedInfoDialogFragment {
      return XposedInfoDialogFragment().putArguments(EXTRA_PACKAGE_NAME to packageName)
    }

    var isShowing = false
  }
}
