package com.absinthe.libchecker.ui.fragment.detail

import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.AppBundleItemBean
import com.absinthe.libchecker.extensions.dp
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.fragment.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.detail.AppBundleBottomSheetView
import com.absinthe.libchecker.view.detail.AppBundleItemView
import java.io.File
import java.util.*

class AppBundleBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<AppBundleBottomSheetView>() {

    private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }
    private lateinit var headerView: BottomSheetHeaderView

    override fun initRootView(): AppBundleBottomSheetView = AppBundleBottomSheetView(requireContext())
    override fun getHeaderView(): BottomSheetHeaderView = headerView

    override fun init() {
        headerView = BottomSheetHeaderView(requireContext()).apply {
            layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.bottomMargin = 24.dp
            }
            title.text = getString(R.string.app_bundle)
        }
        root.adapter.setHeaderView(headerView)
        packageName?.let {
            val packageInfo = PackageUtils.getPackageInfo(it)
            val list = packageInfo.applicationInfo.splitSourceDirs
            val localeList by lazy { Locale.getISOLanguages() }
            val bundleList = if (list.isNullOrEmpty()) {
                emptyList()
            } else {
                list.map { split ->
                    val name = split.substringAfterLast("/")
                    val type = when {
                        name.startsWith("split_config.arm") -> AppBundleItemView.IconType.TYPE_NATIVE_LIBS
                        name.startsWith("split_config.x86") -> AppBundleItemView.IconType.TYPE_NATIVE_LIBS
                        name.endsWith("dpi.apk") -> AppBundleItemView.IconType.TYPE_MATERIALS
                        localeList.contains(name.removePrefix("split_config.").removeSuffix(".apk")) -> AppBundleItemView.IconType.TYPE_STRINGS
                        else -> AppBundleItemView.IconType.TYPE_OTHERS
                    }
                    AppBundleItemBean(name = name, size = File(split).length(), type = type)
                }
            }
            root.adapter.setList(bundleList)
        }
    }
}
