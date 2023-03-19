package com.absinthe.libchecker.ui.main

import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.addCallback
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.databinding.ActivityChartBinding
import com.absinthe.libchecker.ui.fragment.statistics.ChartFragment

class ChartActivity : BaseActivity<ActivityChartBinding>() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setSupportActionBar(binding.toolbar)
    (binding.root as ViewGroup).bringChildToFront(binding.appbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    binding.toolbar.title = getString(R.string.tab_chart)

    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, ChartFragment())
        .commit()
    }

    onBackPressedDispatcher.addCallback(this, true) {
      finish()
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressedDispatcher.onBackPressed()
    }
    return super.onOptionsItemSelected(item)
  }
}
