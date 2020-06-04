package com.absinthe.libchecker.ui.main

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.*
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.librarymap.NativeLibMap
import com.absinthe.libchecker.databinding.ActivityApkDetailBinding
import com.absinthe.libchecker.recyclerview.LibStringAdapter
import com.absinthe.libchecker.utils.ActivityStackManager
import com.absinthe.libchecker.view.dialogfragment.LibDetailDialogFragment
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dongliu.apk.parser.ApkFile
import rikka.core.util.ClipboardUtils
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class ApkDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityApkDetailBinding

    private var tempFile: File? = null
    private val adapter = LibStringAdapter().apply {
        mode = LibStringAdapter.Mode.NATIVE
    }

    init {
        isPaddingToolbar = true
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
        initView()

        intent.data?.scheme?.let { scheme ->
            if (scheme == "content") {
                intent.data?.let { uri ->
                    initData(uri)
                }
            }
        }
    }

    override fun onDestroy() {
        tempFile?.delete()
        super.onDestroy()
    }

    private fun initView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        binding.rvList.apply {
            adapter = this@ApkDetailActivity.adapter
        }

        adapter.apply {
            setEmptyView(R.layout.layout_loading)

            fun openLibDetailDialog(position: Int) {
                if (GlobalValues.config.enableLibDetail) {
                    LibDetailDialogFragment.newInstance(
                        adapter.getItem(position).name,
                        adapter.mode
                    ).apply {
                        ActivityStackManager.topActivity?.apply {
                            show(supportFragmentManager, tag)
                        }
                    }
                }
            }

            setOnItemClickListener { _, _, position ->
                openLibDetailDialog(position)
            }
            setOnItemLongClickListener { _, _, position ->
                ClipboardUtils.put(this@ApkDetailActivity, getItem(position).name)
                ToastUtils.showShort(R.string.toast_copied_to_clipboard)
                true
            }
            setOnItemChildClickListener { _, _, position ->
                openLibDetailDialog(position)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initData(uri: Uri) = lifecycleScope.launch(Dispatchers.IO) {
        val libList = ArrayList<LibStringItem>()
        val inputStream = contentResolver.openInputStream(uri)
        val zipInputStream = ZipInputStream(inputStream)
        tempFile = File(externalCacheDir, "temp.apk")

        FileIOUtils.writeFileFromIS(tempFile, inputStream)

        val apkFile = ApkFile(tempFile)
        withContext(Dispatchers.Main) {
            binding.tvAppName.text = apkFile.apkMeta.label
            binding.tvPackageName.text = apkFile.apkMeta.packageName
            binding.tvVersion.text =
                "${apkFile.apkMeta.versionName} (${apkFile.apkMeta.versionCode})"
            binding.tvTargetApi.text = "API ${apkFile.apkMeta.targetSdkVersion}"
        }

        try {
            val zipFile = ZipFile(tempFile)
            val entries = zipFile.entries()
            var splitName: String
            var next: ZipEntry
            var abi = NO_LIBS

            while (entries.hasMoreElements()) {
                next = entries.nextElement()

                if (next.name.contains("lib/")) {
                    splitName = next.name.split("/").last()

                    if (!libList.any { it.name == splitName }) {
                        libList.add(LibStringItem(splitName, next.size))
                    }

                    if (next.name.contains("arm64-v8a")) {
                        abi = ARMV8
                    } else if (next.name.contains("armeabi-v7a")) {
                        if (abi != ARMV8) {
                            abi = ARMV7
                        }
                    } else if (next.name.contains("armeabi")) {
                        if (abi != ARMV8 && abi != ARMV7) {
                            abi = ARMV5
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                when (abi) {
                    ARMV8 -> {
                        binding.ivAbi.setImageResource(R.drawable.ic_64bit)
                        binding.tvAbiLabel.text = ARMV8_STRING
                    }
                    ARMV7 -> {
                        binding.ivAbi.setImageResource(R.drawable.ic_32bit)
                        binding.tvAbiLabel.text = ARMV7_STRING
                    }
                    ARMV5 -> {
                        binding.ivAbi.setImageResource(R.drawable.ic_32bit)
                        binding.tvAbiLabel.text = ARMV5_STRING
                    }
                    else -> {
                        binding.tvAbiLabel.text = getString(R.string.no_libs)
                    }
                }
            }

            zipFile.close()
        } catch (e: Exception) {
        } finally {
            zipInputStream.close()
            if (libList.isEmpty()) {
                libList.add(LibStringItem(getString(R.string.empty_list), 0))
            } else {
                libList.sortByDescending {
                    NativeLibMap.MAP.containsKey(
                        it.name
                    )
                }
            }

            withContext(Dispatchers.Main) {
                adapter.setNewInstance(libList)
            }
        }
    }
}