package com.absinthe.libchecker.ui.snapshot

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.viewModels
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.databinding.ActivityAlbumBinding
import com.absinthe.libchecker.viewmodel.SnapshotViewModel

class AlbumActivity : BaseActivity() {

    private lateinit var binding: ActivityAlbumBinding
    private val viewModel by viewModels<SnapshotViewModel>()

    override fun setViewBinding(): ViewGroup {
        binding = ActivityAlbumBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnSave.setOnClickListener {
            viewModel.snapshotItems.observe(this, {
                viewModel.backup(0)
            })
        }
    }
}