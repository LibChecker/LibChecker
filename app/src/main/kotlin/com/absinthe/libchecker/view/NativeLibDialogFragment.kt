package com.absinthe.libchecker.view

import android.app.Dialog
import android.os.Bundle
import android.text.SpannableStringBuilder
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.NativeLibMap
import com.absinthe.libchecker.viewholder.LibStringItem
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.util.zip.ZipFile

const val EXTRA_PKG_NAME = "EXTRA_PKG_NAME"

class NativeLibDialogFragment : LCDialogFragment() {

    private lateinit var dialogView: NativeLibView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val packageName: String? = arguments?.getString(EXTRA_PKG_NAME)

        packageName ?: dismiss()

        dialogView = NativeLibView(requireContext())

        val info = Utils.getApp().packageManager.getApplicationInfo(packageName!!, 0)

        dialogView.adapter.setNewInstance(
            getAbiByNativeDir(
                info.sourceDir,
                info.nativeLibraryDir
            ).toMutableList()
        )

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(
                SpannableStringBuilder(
                    String.format(
                        getString(R.string.format_native_libs_title),
                        AppUtils.getAppName(packageName)
                    )
                )
            )
            .setView(dialogView)
            .create()
    }

    private fun getAbiByNativeDir(sourcePath: String, nativePath: String): List<LibStringItem> {
        val file = File(nativePath)
        val list = ArrayList<LibStringItem>()

        file.listFiles()?.let {
            for (abi in it) {
                list.add(LibStringItem(abi.name))
            }
        }

        if (list.isEmpty()) {
            list.addAll(getSourceLibs(sourcePath))
        }

        if (list.isEmpty()) {
            list.add(LibStringItem(getString(R.string.empty_list)))
        } else {
            list.sortByDescending { NativeLibMap.MAP.containsKey(it.name) }
        }
        return list
    }

    private fun getSourceLibs(path: String): ArrayList<LibStringItem> {
        val file = File(path)
        val zipFile = ZipFile(file)
        val entries = zipFile.entries()
        val libList = ArrayList<LibStringItem>()

        while (entries.hasMoreElements()) {
            val name = entries.nextElement().name

            if (name.contains("lib/")) {
                libList.add(LibStringItem(name.split("/").last()))
            }
        }
        zipFile.close()

        return libList
    }
}