package com.absinthe.libchecker.features.chart.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.ActivityChartBinding
import com.absinthe.libchecker.features.chart.ChartViewModel
import com.absinthe.libchecker.services.IWorkerService
import com.absinthe.libchecker.services.WorkerService
import com.absinthe.libchecker.ui.base.BaseActivity
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChartActivity :
  BaseActivity<ActivityChartBinding>(),
  MenuProvider {
  private val viewModel: ChartViewModel by viewModel()
  private var detailedAbiSwitch: MaterialSwitch? = null
  private var isWorkerServiceBound = false
  private var workerBinder: IWorkerService? = null
  private val workerServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      if (service?.pingBinder() == true) {
        workerBinder = IWorkerService.Stub.asInterface(service)
        workerBinder?.initFeatures()
      }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      workerBinder = null
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    addMenuProvider(this, this, Lifecycle.State.CREATED)
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

    isWorkerServiceBound = bindService(
      Intent(this, WorkerService::class.java).apply {
        setPackage(packageName)
      },
      workerServiceConnection,
      BIND_AUTO_CREATE
    )
  }

  override fun onDestroy() {
    if (isWorkerServiceBound) {
      unbindService(workerServiceConnection)
      isWorkerServiceBound = false
    }
    workerBinder = null
    super.onDestroy()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressedDispatcher.onBackPressed()
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.abi_chart_menu, menu)
    val switch = menu.findItem(R.id.action_switch)
      ?.actionView
      ?.findViewById<MaterialSwitch>(R.id.switch_abi_chart)
      ?: return
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
