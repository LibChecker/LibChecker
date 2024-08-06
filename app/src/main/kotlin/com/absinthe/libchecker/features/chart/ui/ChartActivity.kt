package com.absinthe.libchecker.features.chart.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.ActivityChartBinding
import com.absinthe.libchecker.features.chart.ChartViewModel
import com.absinthe.libchecker.ui.base.BaseActivity
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

class ChartActivity :
  BaseActivity<ActivityChartBinding>(),
  MenuProvider {
  private val viewModel by viewModels<ChartViewModel>()
  private var detailedAbiSwitch: MaterialSwitch? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    addMenuProvider(this, this, Lifecycle.State.STARTED)
    setSupportActionBar(binding.toolbar)
    (binding.root as ViewGroup).bringChildToFront(binding.appbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    binding.toolbar.title = getString(R.string.tab_chart)
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.detailAbiSwitchVisibility.collect {
          detailedAbiSwitch?.isVisible = it
        }
      }
    }

    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, ChartFragment())
        .commit()
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressedDispatcher.onBackPressed()
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.abi_chart_menu, menu)
    val menuItem: MenuItem = menu.findItem(R.id.action_switch)
    val switch: MaterialSwitch = menuItem.actionView?.findViewById(R.id.switch_abi_chart)!!
    detailedAbiSwitch = switch

    switch.setOnCheckedChangeListener { button, isChecked ->
      viewModel.setDetailAbiSwitch(isChecked)
      button.text =
        if (isChecked) getString(R.string.chart_abi_detailed) else getString(R.string.chart_abi_concise)
    }
    switch.isChecked = GlobalValues.isDetailedAbiChart
  }

  override fun onMenuItemSelected(menuItem: MenuItem) = false
}
