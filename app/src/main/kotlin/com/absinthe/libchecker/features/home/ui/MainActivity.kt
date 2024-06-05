package com.absinthe.libchecker.features.home.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.activity.viewModels
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.STATUS_INIT_END
import com.absinthe.libchecker.annotation.STATUS_START_INIT
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.databinding.ActivityMainBinding
import com.absinthe.libchecker.features.applist.ui.AppListFragment
import com.absinthe.libchecker.features.home.HomeViewModel
import com.absinthe.libchecker.features.home.INavViewContainer
import com.absinthe.libchecker.features.settings.ui.SettingsFragment
import com.absinthe.libchecker.features.snapshot.ui.SnapshotFragment
import com.absinthe.libchecker.features.statistics.ui.LibReferenceFragment
import com.absinthe.libchecker.services.IWorkerService
import com.absinthe.libchecker.services.OnWorkerListener
import com.absinthe.libchecker.services.WorkerService
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.ui.base.IAppBarContainer
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.extensions.addBackStateHandler
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.setCurrentItem
import com.absinthe.rulesbundle.LCRules
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import jonathanfinerty.once.Once
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

const val PAGE_TRANSFORM_DURATION = 300L

class MainActivity :
  BaseActivity<ActivityMainBinding>(),
  INavViewContainer,
  IAppBarContainer {

  private val appViewModel: HomeViewModel by viewModels()
  private val navViewBehavior by lazy { HideBottomViewOnScrollBehavior<BottomNavigationView>() }
  private val workerListener = object : OnWorkerListener.Stub() {
    override fun onReceivePackagesChanged(packageName: String?, action: String?) {
      if (packageName != null && action != null) {
        if (action == Intent.ACTION_PACKAGE_REMOVED) {
          Timber.d("Package $packageName removed")
        } else {
          Timber.d("Package $packageName changed")
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
  private val _menuProviders = hashSetOf<MenuProvider>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (intent.getBooleanExtra(Constants.PP_FROM_CLOUD_RULES_UPDATE, false)) {
      Timber.w("Reinitializing updated rule database")
      LCRules.init(LibCheckerApp.app)
    }

    initView()
    initObserver()
    bindService(
      Intent(this, WorkerService::class.java).apply {
        setPackage(packageName)
      },
      workerServiceConnection,
      Context.BIND_AUTO_CREATE
    )
    appViewModel.clearApkCache()
    handleIntent(intent)
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

  override fun addMenuProvider(provider: MenuProvider) {
    if (_menuProviders.contains(provider)) {
      super.removeMenuProvider(provider)
    }
    super.addMenuProvider(provider)
  }

  override fun addMenuProvider(provider: MenuProvider, owner: LifecycleOwner) {
    if (_menuProviders.contains(provider)) {
      super.removeMenuProvider(provider)
    }
    super.addMenuProvider(provider, owner)
  }

  override fun addMenuProvider(
    provider: MenuProvider,
    owner: LifecycleOwner,
    state: Lifecycle.State
  ) {
    if (_menuProviders.contains(provider)) {
      super.removeMenuProvider(provider)
    }
    super.addMenuProvider(provider, owner, state)
  }

  override fun removeMenuProvider(provider: MenuProvider) {
    super.removeMenuProvider(provider)
    _menuProviders.remove(provider)
  }

  override fun showNavigationView() {
    Timber.d("showNavigationView")
    // NavigationRailView 不需要隐藏，所以不需要显示
    if (binding.navView is BottomNavigationView) {
      navViewBehavior.slideUp(binding.navView as BottomNavigationView)
    }
  }

  override fun hideNavigationView() {
    Timber.d("hideNavigationView")
    // NavigationRailView 不需要隐藏
    if (binding.navView is BottomNavigationView) {
      navViewBehavior.slideDown(binding.navView as BottomNavigationView)
    }
  }

  override fun showProgressBar() {
    Timber.d("showProgressBar")
    binding.progressHorizontal.show()
  }

  override fun hideProgressBar() {
    Timber.d("hideProgressBar")
    binding.progressHorizontal.hide()
  }

  override fun scheduleAppbarLiftingStatus(isLifted: Boolean) {
    binding.appbar.isLifted = isLifted
  }

  override fun setLiftOnScrollTargetView(targetView: View) {
    binding.appbar.setLiftOnScrollTargetView(targetView)
  }

  private fun initView() {
    val navView = binding.navView as NavigationBarView
    setSupportActionBar(binding.toolbar)
    supportActionBar?.title = LCAppUtils.setTitle(this)

    binding.apply {
      container.bringChildToFront(binding.appbar)
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
            navView.menu.getItem(position).isChecked = true
          }
        })

        // 禁止左右滑动
        isUserInputEnabled = false
        offscreenPageLimit = 2
        fixViewPager2Insets(this)
      }

      navView.apply {
        if (this is BottomNavigationView) {
          (layoutParams as CoordinatorLayout.LayoutParams).also {
            it.behavior = navViewBehavior
          }
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
        if (this is BottomNavigationView) {
          fixBottomNavigationViewInsets(this)
        }
      }
    }

    onBackPressedDispatcher.addBackStateHandler(
      lifecycleOwner = this,
      enabledState = { binding.toolbar.hasExpandedActionView() },
      handler = { binding.toolbar.collapseActionView() }
    )
  }

  /**
   * 覆盖掉 BottomNavigationView 内部的 OnApplyWindowInsetsListener 并避免其被软键盘顶起来
   * @see BottomNavigationView.applyWindowInsets
   */
  private fun fixBottomNavigationViewInsets(view: BottomNavigationView) {
    ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
      // 这里不直接使用 windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
      // 因为它的结果可能受到 insets 传播链上层某环节的影响，出现了错误的 navigationBarsInsets
      // 使用 WindowInsetsCompat.Type.systemBars() 以适配如 HyperOS Freeform 之类的奇怪的东西
      val navigationBarsInsets =
        ViewCompat.getRootWindowInsets(view)!!.getInsets(WindowInsetsCompat.Type.systemBars())
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

      effect.onEach {
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
              doOnMainThreadIdle {
                showNavigationView()
              }
              initFeatures()
            }
          }

          else -> {}
        }
      }.launchIn(lifecycleScope)
    }
  }

  private fun initFeatures() {
    lifecycleScope.launch {
      while (appViewModel.workerBinder == null) {
        delay(300)
      }

      withContext(Dispatchers.Main) {
        Timber.d("initFeatures")
        appViewModel.workerBinder?.initFeatures()
      }
    }
  }
}
