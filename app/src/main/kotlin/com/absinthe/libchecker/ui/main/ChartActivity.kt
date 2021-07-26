package com.absinthe.libchecker.ui.main

import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.databinding.ActivityChartBinding
import com.absinthe.libchecker.ui.fragment.statistics.ChartFragment

class ChartActivity : BaseActivity() {
  private lateinit var binding: ActivityChartBinding

  override fun setViewBinding(): ViewGroup {
    binding = ActivityChartBinding.inflate(layoutInflater)
    return binding.root
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setAppBar(binding.appbar, binding.toolbar)
    (binding.root as ViewGroup).bringChildToFront(binding.appbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, ChartFragment())
        .commit()
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressed()
    }
    return super.onOptionsItemSelected(item)
  }
}
