package com.absinthe.libchecker.view.dialogfragment

import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.SpannableStringBuilder
import com.absinthe.libchecker.R
import com.absinthe.libchecker.view.LCDialogFragment
import com.absinthe.libchecker.view.NativeLibView
import com.absinthe.libchecker.viewholder.LibStringItem
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

const val EXTRA_PKG_NAME = "EXTRA_PKG_NAME"

class NativeLibDialogFragment : LCDialogFragment() {

    private lateinit var dialogView: NativeLibView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val packageName: String? = arguments?.getString(EXTRA_PKG_NAME)
        packageName ?: dismiss()

        dialogView = NativeLibView(requireContext())
            .apply {
            tvTitle.text = SpannableStringBuilder(
                String.format(
                    getString(R.string.format_native_libs_title),
                    AppUtils.getAppName(packageName)
                )
            )
        }

        GlobalScope.launch(Dispatchers.IO) {
            val list = mutableListOf<LibStringItem>()

            try {
                val info = Utils.getApp().packageManager.getApplicationInfo(packageName!!, 0)

                list.addAll(getAbiByNativeDir(
                    info.sourceDir,
                    info.nativeLibraryDir
                ))

                //Fix Dialog can't display all items
                if (list.size > 10) {
                    list.add(LibStringItem("", 0))
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                list.add(LibStringItem("Not found", 0))
            }

            withContext(Dispatchers.Main) {
                dialogView.adapter.setNewInstance(list)
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
    }

    private fun getAbiByNativeDir(sourcePath: String, nativePath: String): List<LibStringItem> {
        val file = File(nativePath)
        val list = ArrayList<LibStringItem>()

        file.listFiles()?.let {
            for (abi in it) {
                list.add(LibStringItem(abi.name, abi.length()))
            }
        }

        if (list.isEmpty()) {
            list.addAll(getSourceLibs(sourcePath))
        }

        if (list.isEmpty()) {
            list.add(LibStringItem(getString(R.string.empty_list), 0))
        } else {
            list.sortByDescending { it.size }
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
                libList.add(LibStringItem(name.split("/").last(), entries.nextElement().size))
            }
        }
        zipFile.close()

        return libList
    }
}