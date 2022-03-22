package com.absinthe.libchecker.ui.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.viewModels
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.STATUS_INIT_END
import com.absinthe.libchecker.annotation.STATUS_START_INIT
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.databinding.ActivityMainBinding
import com.absinthe.libchecker.services.IWorkerService
import com.absinthe.libchecker.services.OnWorkerListener
import com.absinthe.libchecker.services.WorkerService
import com.absinthe.libchecker.ui.fragment.applist.AppListFragment
import com.absinthe.libchecker.ui.fragment.settings.SettingsFragment
import com.absinthe.libchecker.ui.fragment.snapshot.SnapshotFragment
import com.absinthe.libchecker.ui.fragment.statistics.LibReferenceFragment
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.setCurrentItem
import com.absinthe.libchecker.viewmodel.HomeViewModel
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import jonathanfinerty.once.Once
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

const val PAGE_TRANSFORM_DURATION = 300L

class MainActivity : BaseActivity<ActivityMainBinding>(), INavViewContainer {

  private val appViewModel: HomeViewModel by viewModels()
  private val navViewBehavior by lazy { HideBottomViewOnScrollBehavior<BottomNavigationView>() }
  private val workerListener = object : OnWorkerListener.Stub() {
    override fun onReceivePackagesChanged(packageName: String?, action: String?) {
      appViewModel.packageChanged(packageName.orEmpty(), action.orEmpty())
    }
  }
  private val workerServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      workerBinder = IWorkerService.Stub.asInterface(service)
      workerBinder?.registerOnWorkerListener(workerListener)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      workerBinder = null
    }
  }
  private var workerBinder: IWorkerService? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    initView()
    preWork()
    bindService(
      Intent(this, WorkerService::class.java).apply {
        setPackage(packageName)
      },
      workerServiceConnection,
      Context.BIND_AUTO_CREATE
    )
    handleIntentFromShortcuts(intent)
    initObserver()
    clearApkCache()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntentFromShortcuts(intent)
  }

  override fun onResume() {
    super.onResume()
    if (GlobalValues.shouldRequestChange.value == true) {
      appViewModel.requestChange(true)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    workerBinder?.unregisterOnWorkerListener(workerListener)
    unbindService(workerServiceConnection)
  }

  override fun showNavigationView() {
    Timber.d("showNavigationView")
    navViewBehavior.slideUp(binding.navView)
  }

  override fun hideNavigationView() {
    Timber.d("hideNavigationView")
    navViewBehavior.slideDown(binding.navView)
  }

  private fun initView() {
    setAppBar(binding.appbar, binding.toolbar)
    binding.root.bringChildToFront(binding.appbar)
    supportActionBar?.title = LCAppUtils.setTitle(this)

    binding.apply {
      viewpager.apply {
        adapter = object : FragmentStateAdapter(this@MainActivity) {
          override fun getItemCount(): Int {
            return 4
          }

          override fun createFragment(position: Int): Fragment {
            return when (position) {
              0 -> AppListFragment()
              1 -> LibReferenceFragment()
              2 -> SnapshotFragment()
              else -> SettingsFragment()
            }
          }
        }

        // 当ViewPager切换页面时，改变底部导航栏的状态
        registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
          override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            binding.navView.menu.getItem(position).isChecked = true
          }
        })

        // 禁止左右滑动
        isUserInputEnabled = false
        offscreenPageLimit = 2
      }

      navView.apply {
        (layoutParams as CoordinatorLayout.LayoutParams).also {
          it.behavior = navViewBehavior
        }
        requestLayout()
        // 当 ViewPager 切换页面时，改变 ViewPager 的显示
        setOnItemSelectedListener {

          fun performClickNavigationItem(index: Int) {
            if (binding.viewpager.currentItem != index) {
              if (!binding.viewpager.isFakeDragging) {
                binding.viewpager.setCurrentItem(index, PAGE_TRANSFORM_DURATION)
              }
            } else {
              val clickFlag =
                binding.viewpager.getTag(R.id.viewpager_tab_click) as? Boolean ?: false
              if (!clickFlag) {
                binding.viewpager.setTag(R.id.viewpager_tab_click, true)

                lifecycleScope.launch {
                  delay(200)
                  binding.viewpager.setTag(R.id.viewpager_tab_click, false)
                }
              } else if (appViewModel.controller?.isAllowRefreshing() == true) {
                appViewModel.controller?.onReturnTop()
              }
            }
          }

          when (it.itemId) {
            R.id.navigation_app_list -> performClickNavigationItem(0)
            R.id.navigation_classify -> performClickNavigationItem(1)
            R.id.navigation_snapshot -> performClickNavigationItem(2)
            R.id.navigation_settings -> performClickNavigationItem(3)
          }
          true
        }
        setOnClickListener { /*Do nothing*/ }
      }
    }
  }

  private fun handleIntentFromShortcuts(intent: Intent) {
    when (intent.action) {
      Constants.ACTION_APP_LIST -> binding.viewpager.setCurrentItem(0, false)
      Constants.ACTION_STATISTICS -> binding.viewpager.setCurrentItem(1, false)
      Constants.ACTION_SNAPSHOT -> binding.viewpager.setCurrentItem(2, false)
    }
    Analytics.trackEvent(
      Constants.Event.LAUNCH_ACTION,
      EventProperties().set("Action", intent.action)
    )
  }

  private fun initObserver() {
    appViewModel.apply {
      if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)) {
        initItems()
      } else {
        lifecycleScope.launch(Dispatchers.IO) {
          do {
            workerBinder?.initKotlinUsage()
            delay(300)
          } while (workerBinder == null)
        }
      }

      lifecycleScope.launchWhenStarted {
        effect.collect {
          when (it) {
            is HomeViewModel.Effect.ReloadApps -> {
              binding.viewpager.setCurrentItem(0, true)
            }
            is HomeViewModel.Effect.UpdateAppListStatus -> {
              if (it.status == STATUS_START_INIT) {
                doOnMainThreadIdle {
                  hideNavigationView()
                }
              } else if (it.status == STATUS_INIT_END) {
                lifecycleScope.launch(Dispatchers.IO) {
                  do {
                    workerBinder?.initKotlinUsage()
                    delay(300)
                  } while (workerBinder == null)
                }
              }
            }
            else -> {}
          }
        }
      }
    }
  }

  private fun clearApkCache() {
    FileUtils.delete(File(externalCacheDir, Constants.TEMP_PACKAGE))
  }

  private fun preWork() {
    lifecycleScope.launch(Dispatchers.IO) {
      if (AppItemRepository.shouldClearDiffItemsInDatabase) {
        Repositories.lcRepository.deleteAllSnapshotDiffItems()
      }
    }
  }
}
