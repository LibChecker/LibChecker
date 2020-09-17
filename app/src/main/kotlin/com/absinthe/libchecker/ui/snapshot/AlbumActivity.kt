package com.absinthe.libchecker.ui.snapshot

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.ActivityAlbumBinding
import com.absinthe.libchecker.utils.StorageUtils
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AlbumActivity : BaseActivity() {

    private lateinit var binding: ActivityAlbumBinding
    private val viewModel by viewModels<SnapshotViewModel>()

    override fun setViewBinding(): ViewGroup {
        binding = ActivityAlbumBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()
    }

    private fun initView() {
        setAppBar(binding.appbar, binding.toolbar)
        (binding.root as ViewGroup).bringChildToFront(binding.appbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnSave.setOnClickListener {
            viewModel.snapshotItems.observe(this, {
                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                val date = Date(GlobalValues.snapshotTimestamp)
                val formatted = simpleDateFormat.format(date)

                if (StorageUtils.isExternalStorageWritable) {
                    StorageUtils.createFile(this, "*/*",
                        "LibChecker-Snapshot-Backups-$formatted.lcss"
                    )
                }
            })
        }
        binding.btnDelete.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val timeStampList = viewModel.repository.getTimeStamps()
                val charList = mutableListOf<String>()
                timeStampList.forEach { charList.add(viewModel.getFormatDateString(it.timestamp)) }

                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(this@AlbumActivity)
                        .setTitle(R.string.dialog_title_change_timestamp)
                        .setItems(charList.toTypedArray()) { _, which ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                viewModel.repository.deleteSnapshotsAndTimeStamp(timeStampList[which].timestamp)
                            }
                            GlobalValues.snapshotTimestamp = if (timeStampList.isEmpty()) {
                                0L
                            } else {
                                timeStampList[0].timestamp
                            }
                        }
                        .show()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Constants.REQUEST_CODE_BACKUP && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                try {
                    contentResolver.openOutputStream(it)?.let { os ->
                        viewModel.backup(os, GlobalValues.snapshotTimestamp)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}