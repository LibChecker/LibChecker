package com.absinthe.libchecker.view

import android.app.Dialog
import android.os.Bundle
import android.text.Html
import androidx.fragment.app.DialogFragment
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.NativeLibMap
import com.absinthe.libchecker.viewholder.LibStringItem
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

const val EXTRA_PKG_NAME = "EXTRA_PKG_NAME"

class NativeLibDialogFragment : DialogFragment() {

    private lateinit var dialogView: NativeLibView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val packageName:String? = arguments?.getString(EXTRA_PKG_NAME)

        dialogView = NativeLibView(requireContext())

        val info = Utils.getApp().packageManager.getApplicationInfo(packageName!!, 0)

        dialogView.adapter.items = getAbiByNativeDir(info.nativeLibraryDir)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(Html.fromHtml(String.format(getString(R.string.format_native_libs_title), AppUtils.getAppName(packageName))))
            .setView(dialogView)
            .create()
    }

    private fun getAbiByNativeDir(nativePath: String): List<LibStringItem> {
        val file = File(nativePath)
        val list = ArrayList<LibStringItem>()

        file.listFiles()?.let {
            for (abi in it) {
                list.add(LibStringItem(abi.name))
            }
        }

        list.sortByDescending { NativeLibMap.MAP.containsKey(it.name) }
        return list
    }
}