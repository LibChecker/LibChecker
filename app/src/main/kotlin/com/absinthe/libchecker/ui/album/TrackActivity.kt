package com.absinthe.libchecker.ui.album

import android.os.Bundle
import android.view.ViewGroup
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.databinding.ActivityTrackBinding

class TrackActivity : BaseActivity() {

    private lateinit var binding: ActivityTrackBinding

    override fun setViewBinding(): ViewGroup {
        binding = ActivityTrackBinding.inflate(layoutInflater)
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
    }
}