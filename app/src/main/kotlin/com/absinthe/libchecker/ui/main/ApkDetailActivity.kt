package com.absinthe.libchecker.ui.main

import android.os.Bundle
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.databinding.ActivityApkDetailBinding
import com.absinthe.libchecker.ktx.logd
import com.absinthe.libchecker.recyclerview.LibStringAdapter
import java.util.zip.ZipInputStream

class ApkDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityApkDetailBinding
    private val adapter = LibStringAdapter().apply {
        mode = LibStringAdapter.Mode.NATIVE
    }

    override fun setViewBinding() {
        binding = ActivityApkDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setRoot() {
        root = binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.rvList.apply {
            adapter = this@ApkDetailActivity.adapter
        }

        intent.data?.scheme?.let { scheme ->
            if (scheme == "content") {
                intent.data?.let { uri ->
                    logd(uri.toString())

                    val libList = ArrayList<LibStringItem>()
                    val inputStream = contentResolver.openInputStream(uri)
                    val zipInputStream = ZipInputStream(inputStream)

                    try {
                        var entry = zipInputStream.nextEntry
                        var splitName: String

                        while (entry != null) {
                            if (entry.name.contains("lib/")) {
                                splitName = entry.name.split("/").last()

                                if (!libList.any { it.name == splitName }) {
                                    logd(splitName)
                                    libList.add(LibStringItem(splitName, entry.size))
                                }
                            }
                            entry = zipInputStream.nextEntry
                        }
                    } catch (e: Exception) {
                    } finally {
                        zipInputStream.close()
                        adapter.setNewInstance(libList)
                    }
                }
            }
        }
    }
}