package com.absinthe.libchecker.features.statistics.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.AUTUMN
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.SPRING
import com.absinthe.libchecker.annotation.SUMMER
import com.absinthe.libchecker.annotation.WINTER
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.ActivityLibReferenceBinding
import com.absinthe.libchecker.features.applist.ui.adapter.AppAdapter
import com.absinthe.libchecker.features.statistics.LibReferenceViewModel
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.utils.extensions.isOrientationLandscape
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.paddingTopCompat
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import java.io.File
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import rikka.widget.borderview.BorderView

const val EXTRA_REF_NAME = "REF_NAME"
const val EXTRA_REF_LABEL = "REF_LABEL"
const val EXTRA_REF_TYPE = "REF_TYPE"
const val EXTRA_REF_LIST = "REF_LIST"

class LibReferenceActivity : BaseActivity<ActivityLibReferenceBinding>() {

  private val adapter = AppAdapter()
  private val viewModel: LibReferenceViewModel by viewModels()
  private val refName by lazy { intent.extras?.getString(EXTRA_REF_NAME) }
  private val refLabel by lazy { intent.extras?.getString(EXTRA_REF_LABEL) }
  private val refType by lazy { intent.extras?.getInt(EXTRA_REF_TYPE) ?: NATIVE }
  private val refList by lazy { intent.extras?.getStringArray(EXTRA_REF_LIST) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initView()
    initData()
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
      fitsSystemWindows = newConfig.isOrientationLandscape
      paddingTopCompat = 0
    }
  }

  private fun initView() {
    setSupportActionBar(binding.toolbar)
    supportActionBar?.apply {
      refLabel?.let {
        title = it
      }
      subtitle = refName
    }
    binding.apply {
      root.apply {
        fitsSystemWindows = isOrientationLandscape
        paddingTopCompat = 0
      }

      supportActionBar?.setDisplayHomeAsUpEnabled(true)
      (root as ViewGroup).bringChildToFront(appbar)

      list.apply {
        adapter = this@LibReferenceActivity.adapter
        borderVisibilityChangedListener =
          BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
            binding.appbar.isLifted = !top
          }
        setHasFixedSize(true)
        FastScrollerBuilder(this).useMd2Style().build()
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
        (root as ViewGroup).bringChildToFront(appbar)
      }
      lottie.apply {
        imageAssetsFolder = File.separator

        val assetName = when (GlobalValues.season) {
          SPRING -> "anim/lib_reference_spring.json.zip"
          SUMMER -> "anim/lib_reference_summer.json.zip"
          AUTUMN -> "anim/lib_reference_autumn.json.zip"
          WINTER -> "anim/lib_reference_winter.json.zip"
          else -> "anim/lib_reference_summer.json.zip"
        }

        setAnimation(assetName)
      }
    }

    viewModel.libRefListFlow.onEach {
      adapter.setList(it)
      if (binding.vfContainer.displayedChild != 1) {
        binding.vfContainer.displayedChild = 1
      }
    }.launchIn(lifecycleScope)

    adapter.setOnItemClickListener { _, view, position ->
      if (AntiShakeUtils.isInvalidClick(view)) {
        return@setOnItemClickListener
      }
      launchDetailPage(
        item = adapter.getItem(position),
        refName = refName,
        refType = refType
      )
    }
  }

  private fun initData() {
    this.refName?.let { name ->
      refList?.toList()?.let(viewModel::setData) ?: viewModel.setData(name, refType)
    } ?: finish()
  }
}
