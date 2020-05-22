package com.absinthe.libchecker.view.dialogfragment

import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.core.view.isGone
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.view.LCDialogFragment
import com.absinthe.libchecker.view.NativeLibView
import com.blankj.utilcode.util.AppUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                val info = requireContext().packageManager.getApplicationInfo(packageName!!, 0)

                list.addAll(PackageUtils.getAbiByNativeDir(
                    info.sourceDir,
                    info.nativeLibraryDir
                ))

                if (list.isEmpty()) {
                    list.add(LibStringItem(getString(R.string.empty_list), 0))
                }

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
                dialogView.ibSort.isGone = list.size == 1
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
    }
}