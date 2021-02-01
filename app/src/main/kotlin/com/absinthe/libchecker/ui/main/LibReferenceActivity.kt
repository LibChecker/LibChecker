package com.absinthe.libchecker.ui.main

import android.app.ActivityOptions
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.view.Window
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.ActivityLibReferenceBinding
import com.absinthe.libchecker.extensions.*
import com.absinthe.libchecker.recyclerview.adapter.AppAdapter
import com.absinthe.libchecker.ui.detail.AppDetailActivity
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.viewmodel.LibReferenceViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.libraries.utils.utils.UiUtils
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.material.widget.BorderView

const val EXTRA_NAME = "NAME"
const val EXTRA_TYPE = "TYPE"

class LibReferenceActivity : BaseActivity() {

    private lateinit var binding: ActivityLibReferenceBinding
    private val adapter = AppAdapter()
    private val viewModel by viewModels<LibReferenceViewModel>()

    override fun setViewBinding(): ViewGroup {
        binding = ActivityLibReferenceBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.apply {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            sharedElementsUseOverlay = false
        }
        setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        super.onCreate(savedInstanceState)

        val name = intent.extras?.getString(EXTRA_NAME)
        val type = intent.extras?.getInt(EXTRA_TYPE) ?: NATIVE

        if (name == null) {
            finish()
        } else {
            initView()
            viewModel.dbItems.observe(this, {
                viewModel.setData(name, type)
            })

            lifecycleScope.launch(Dispatchers.IO) {
                LCAppUtils.getRuleWithRegex(name, type)?.let {
                    withContext(Dispatchers.Main) {
                        binding.toolbar.title = it.label
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.release()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.root.apply {
            fitsSystemWindows = isOrientationLandscape
            paddingTopCompat = 0
        }
    }

    override fun onBackPressed() {
        finish()
    }

    private fun initView() {
        binding.apply {
            root.apply {
                fitsSystemWindows = isOrientationLandscape
                paddingTopCompat = 0
            }

            setAppBar(appbar, toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            (root as ViewGroup).bringChildToFront(appbar)

            rvList.apply {
                adapter = this@LibReferenceActivity.adapter
                borderVisibilityChangedListener =
                    BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
                        appBar?.setRaised(!top)
                    }
                paddingBottomCompat = UiUtils.getNavBarHeight(windowManager)
                setHasFixedSize(true)
                addPaddingTop(UiUtils.getStatusBarHeight())
            }
            vfContainer.apply {
                setInAnimation(
                    this@LibReferenceActivity,
                    R.anim.anim_fade_in
                )
                setOutAnimation(
                    this@LibReferenceActivity,
                    R.anim.anim_fade_out
                )
                displayedChild = 0
            }
            lottie.apply {
                imageAssetsFolder = "/"

                val assetName = when(GlobalValues.season) {
                    SPRING -> "lib_reference_spring.json"
                    SUMMER -> "lib_reference_summer.json"
                    AUTUMN -> "lib_reference_autumn.json"
                    WINTER -> "lib_reference_winter.json"
                    else -> "lib_reference_summer.json"
                }

                setAnimation(assetName)
            }
        }

        viewModel.libRefList.observe(this, {
            adapter.setList(it)
            binding.vfContainer.displayedChild = 1
        })

        adapter.setOnItemClickListener { _, view, position ->
            if (AntiShakeUtils.isInvalidClick(view)) {
                return@setOnItemClickListener
            }

            val intent = Intent(this, AppDetailActivity::class.java).apply {
                putExtras(Bundle().apply {
                    putString(EXTRA_PACKAGE_NAME, adapter.getItem(position).packageName)
                })
            }

            val options = ActivityOptions.makeSceneTransitionAnimation(
                this, view, view.transitionName
            )

            if (GlobalValues.isShowEntryAnimation.valueUnsafe) {
                startActivity(intent, options.toBundle())
            } else {
                startActivity(intent)
            }
        }
    }
}
