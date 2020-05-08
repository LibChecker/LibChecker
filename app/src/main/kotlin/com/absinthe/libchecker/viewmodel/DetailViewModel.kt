package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.viewholder.LibStringItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    val libItems: MutableLiveData<ArrayList<LibStringItem>> = MutableLiveData()

    fun initData(context: Context, packageName: String) = viewModelScope.launch(Dispatchers.IO) {
        val list = ArrayList<LibStringItem>()

        try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)

            list.addAll(
                getAbiByNativeDir(
                    context,
                    info.sourceDir,
                    info.nativeLibraryDir
                )
            )
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            list.add(LibStringItem("Not found", 0))
        }

        withContext(Dispatchers.Main) {
            libItems.value = list
        }
    }

    private fun getAbiByNativeDir(
        context: Context,
        sourcePath: String,
        nativePath: String
    ): List<LibStringItem> {
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
            list.add(LibStringItem(context.getString(R.string.empty_list), 0))
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