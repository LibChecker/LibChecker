package com.absinthe.libchecker.ui.main

import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.view.Window
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.AppItem
import com.absinthe.libchecker.constant.AppItemRepository
import com.absinthe.libchecker.databinding.ActivityLibReferenceBinding
import com.absinthe.libchecker.recyclerview.AppAdapter
import com.absinthe.libchecker.utils.AntiShakeUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.view.dialogfragment.EXTRA_PKG_NAME
import com.blankj.utilcode.util.BarUtils
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.material.widget.BorderView

const val TYPE_ALL = 0
const val TYPE_NATIVE = 1
const val TYPE_SERVICE = 2
const val TYPE_ACTIVITY = 3
const val TYPE_BROADCAST_RECEIVER = 4
const val TYPE_CONTENT_PROVIDER = 5

const val EXTRA_NAME = "NAME"
const val EXTRA_TYPE = "TYPE"

class LibReferenceActivity : BaseActivity() {

    private lateinit var binding: ActivityLibReferenceBinding
    private val adapter = AppAdapter()

    override fun setViewBinding() {
        binding = ActivityLibReferenceBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setRoot() {
        root = binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.apply {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            sharedElementsUseOverlay = false
        }
        setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        super.onCreate(savedInstanceState)

        setAppBar(binding.appbar, binding.toolbar)
        (binding.root as ViewGroup).bringChildToFront(binding.appbar)

        binding.rvList.apply {
            adapter = this@LibReferenceActivity.adapter
            borderVisibilityChangedListener =
                BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
                    appBar?.setRaised(!top)
                }
            setPadding(0, paddingTop + BarUtils.getStatusBarHeight(), 0, UiUtils.getNavBarHeight())
        }
        binding.vfContainer.apply {
            setInAnimation(
                this@LibReferenceActivity,
                R.anim.anim_fade_in
            )
            setOutAnimation(
                this@LibReferenceActivity,
                R.anim.anim_fade_out
            )
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter.setOnItemClickListener { _, view, position ->
            if (AntiShakeUtils.isInvalidClick(view)) {
                return@setOnItemClickListener
            }

            val intent = Intent(this, AppDetailActivity::class.java).apply {
                putExtras(Bundle().apply {
                    putString(EXTRA_PKG_NAME, adapter.getItem(position).packageName)
                })
            }

            val options = ActivityOptions.makeSceneTransitionAnimation(
                this,
                view,
                "app_card_container"
            )
            startActivity(intent, options.toBundle())
        }

        val name = intent.extras?.getString(EXTRA_NAME)
        val type = intent.extras?.getInt(EXTRA_TYPE)

        if (name == null || type == null) {
            finish()
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                setData(name, type)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private suspend fun setData(name: String, type: Int) {

        withContext(Dispatchers.Main) {
            binding.vfContainer.displayedChild = 0
        }

        val list = mutableListOf<AppItem>()

        AppItemRepository.allItems.value?.let { items ->
            try {
                if (type == TYPE_NATIVE) {
                    for (item in items) {
                        val packageInfo = PackageUtils.getPackageInfo(item.packageName)

                        val natives = PackageUtils.getAbiByNativeDir(
                            packageInfo.applicationInfo.sourceDir,
                            packageInfo.applicationInfo.nativeLibraryDir
                        )

                        for (native in natives) {
                            if (native.name == name) {
                                list.add(item)
                                break
                            }
                        }
                    }
                } else if (type == TYPE_SERVICE) {
                    for (item in items) {
                        val packageInfo =
                            packageManager.getPackageInfo(
                                item.packageName,
                                PackageManager.GET_SERVICES
                            )
                        packageInfo.services?.let { services ->
                            for (service in services) {
                                if (service.name == name) {
                                    list.add(item)
                                    break
                                }
                            }
                        }
                    }
                } else if (type == TYPE_ACTIVITY) {
                    for (item in items) {
                        val packageInfo =
                            packageManager.getPackageInfo(
                                item.packageName,
                                PackageManager.GET_ACTIVITIES
                            )
                        packageInfo.activities?.let { activities ->
                            for (activity in activities) {
                                if (activity.name == name) {
                                    list.add(item)
                                    break
                                }
                            }
                        }
                    }
                } else if (type == TYPE_BROADCAST_RECEIVER) {
                    for (item in items) {
                        val packageInfo =
                            packageManager.getPackageInfo(
                                item.packageName,
                                PackageManager.GET_RECEIVERS
                            )
                        packageInfo.receivers?.let { receivers ->
                            for (receiver in receivers) {
                                if (receiver.name == name) {
                                    list.add(item)
                                    break
                                }
                            }
                        }
                    }
                } else if (type == TYPE_CONTENT_PROVIDER) {
                    for (item in items) {
                        val packageInfo =
                            packageManager.getPackageInfo(
                                item.packageName,
                                PackageManager.GET_PROVIDERS
                            )
                        packageInfo.providers?.let { providers ->
                            for (provider in providers) {
                                if (provider.name == name) {
                                    list.add(item)
                                    break
                                }
                            }
                        }
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }
        withContext(Dispatchers.Main) {
            adapter.setNewInstance(list)
            binding.vfContainer.displayedChild = 1
        }
    }
}
