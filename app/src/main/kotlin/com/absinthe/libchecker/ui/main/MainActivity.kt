package com.absinthe.libchecker.ui.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.STATUS_INIT_END
import com.absinthe.libchecker.annotation.STATUS_START_INIT
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.databinding.ActivityMainBinding
import com.absinthe.libchecker.services.IWorkerService
import com.absinthe.libchecker.services.OnWorkerListener
import com.absinthe.libchecker.services.WorkerService
import com.absinthe.libchecker.ui.fragment.IAppBarContainer
import com.absinthe.libchecker.ui.fragment.applist.AppListFragment
import com.absinthe.libchecker.ui.fragment.settings.SettingsFragment
import com.absinthe.libchecker.ui.fragment.snapshot.SnapshotFragment
import com.absinthe.libchecker.ui.fragment.statistics.LibReferenceFragment
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.setCurrentItem
import com.absinthe.libchecker.viewmodel.HomeViewModel
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import java.io.File
import jonathanfinerty.once.Once
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

const val PAGE_TRANSFORM_DURATION = 300L

class MainActivity : BaseActivity<ActivityMainBinding>(), INavViewContainer, IAppBarContainer {

  private val appViewModel: HomeViewModel by viewModels()
  private val navViewBehavior by lazy { HideBottomViewOnScrollBehavior<BottomNavigationView>() }
  private val workerListener = object : OnWorkerListener.Stub() {
    override fun onReceivePackagesChanged(packageName: String?, action: String?) {
      if (packageName != null && action != null) {
        if (action == Intent.ACTION_PACKAGE_REMOVED) {
          AppItemRepository.allPackageInfoMap.remove(packageName)
        } else {
          runCatching {
            PackageUtils.getPackageInfo(
              packageName,
              PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS
            )
          }.onSuccess {
            AppItemRepository.allPackageInfoMap[packageName] = it
          }
        }
      }
      appViewModel.packageChanged(packageName.orEmpty(), action.orEmpty())
    }
  }
  private val workerServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      if (service?.pingBinder() == true) {
        appViewModel.workerBinder = IWorkerService.Stub.asInterface(service)
        runCatching {
          appViewModel.workerBinder?.registerOnWorkerListener(workerListener)
        }.onFailure {
          Timber.e(it)
        }
      }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      appViewModel.workerBinder = null
    }
  }
  private var _menuProvider: MenuProvider? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    initView()
    bindService(
      Intent(this, WorkerService::class.java).apply {
        setPackage(packageName)
      },
      workerServiceConnection,
      Context.BIND_AUTO_CREATE
    )
    handleIntent(intent)
    initObserver()
    clearApkCache()
    onBackPressedDispatcher.addCallback(this, true) {
      val closeBtn = findViewById<View>(androidx.appcompat.R.id.search_close_btn)
      if (closeBtn != null) {
        binding.toolbar.collapseActionView()
      } else {
        finish()
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  override fun onDestroy() {
    super.onDestroy()
    appViewModel.workerBinder?.unregisterOnWorkerListener(workerListener)
    unbindService(workerServiceConnection)
  }

  override fun removeMenuProvider(provider: MenuProvider) {
    if (hasWindowFocus()) {
      super.removeMenuProvider(provider)
    }
  }

  override fun showNavigationView() {
    Timber.d("showNavigationView")
    navViewBehavior.slideUp(binding.navView)
  }

  override fun hideNavigationView() {
    Timber.d("hideNavigationView")
    navViewBehavior.slideDown(binding.navView)
  }

  override fun showProgressBar() {
    Timber.d("showProgressBar")
    binding.progressHorizontal.show()
  }

  override fun hideProgressBar() {
    Timber.d("hideProgressBar")
    binding.progressHorizontal.hide()
  }

  override var currentMenuProvider: MenuProvider?
    get() = _menuProvider
    set(value) {
      _menuProvider = value
    }

  override fun scheduleAppbarLiftingStatus(isLifted: Boolean) {
    binding.appbar.isLifted = isLifted
  }

  override fun setLiftOnScrollTargetView(targetView: View) {
    binding.appbar.setLiftOnScrollTargetView(targetView)
  }

  private fun initView() {
    setSupportActionBar(binding.toolbar)
    supportActionBar?.title = LCAppUtils.setTitle(this)

    binding.apply {
      root.bringChildToFront(binding.appbar)
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
        fixViewPager2Insets(this)
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
        fixBottomNavigationViewInsets(this)
      }
    }
  }

  /**
   * 覆盖掉 BottomNavigationView 内部的 OnApplyWindowInsetsListener 并避免其被软键盘顶起来
   * @see BottomNavigationView.applyWindowInsets
   */
  private fun fixBottomNavigationViewInsets(view: BottomNavigationView) {
    ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
      // 这里不直接使用 windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
      // 因为它的结果可能受到 insets 传播链上层某环节的影响，出现了错误的 navigationBarsInsets
      val navigationBarsInsets =
        ViewCompat.getRootWindowInsets(view)!!.getInsets(WindowInsetsCompat.Type.navigationBars())
      view.updatePadding(bottom = navigationBarsInsets.bottom)
      windowInsets
    }
  }

  private fun fixViewPager2Insets(view: ViewPager2) {
    ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
      /* Do nothing */
      windowInsets
    }
  }

  private fun handleIntent(intent: Intent) {
    when (intent.action) {
      Constants.ACTION_APP_LIST -> binding.viewpager.setCurrentItem(0, false)
      Constants.ACTION_STATISTICS -> binding.viewpager.setCurrentItem(1, false)
      Constants.ACTION_SNAPSHOT -> binding.viewpager.setCurrentItem(2, false)
      Intent.ACTION_APPLICATION_PREFERENCES -> binding.viewpager.setCurrentItem(3, false)
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
        initFeatures()
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
                initFeatures()
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

  private fun initFeatures() {
    Handler(Looper.getMainLooper()).also {
      it.post(object : Runnable {
        override fun run() {
          if (appViewModel.workerBinder == null) {
            it.postDelayed(this, 300)
          } else {
            Timber.d("initFeatures")
            appViewModel.workerBinder?.initFeatures()
          }
        }
      })
    }
  }
}
