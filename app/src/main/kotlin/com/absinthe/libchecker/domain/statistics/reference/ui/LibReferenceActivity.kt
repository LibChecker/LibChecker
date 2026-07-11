package com.absinthe.libchecker.domain.statistics.reference.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.databinding.ActivityLibReferenceBinding
import com.absinthe.libchecker.domain.app.list.model.AppListRenderState
import com.absinthe.libchecker.domain.app.list.ui.adapter.AppAdapter
import com.absinthe.libchecker.domain.statistics.reference.presentation.LibReferenceViewModel
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.utils.extensions.applySystemBarsPadding
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel
import rikka.widget.borderview.BorderView

const val EXTRA_REF_NAME = "REF_NAME"
const val EXTRA_REF_LABEL = "REF_LABEL"
const val EXTRA_REF_TYPE = "REF_TYPE"
const val EXTRA_REF_LIST = "REF_LIST"

class LibReferenceActivity : BaseActivity<ActivityLibReferenceBinding>() {

  private val adapter = AppAdapter()
  private val viewModel: LibReferenceViewModel by viewModel()
  private var refName: String? = null
  private var refLabel: String? = null
  private var refType: Int = NATIVE
  private var refList: Array<String>? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    readIntentExtras()
    initView()
    initData()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    readIntentExtras()
    updateActionBar()
    initData()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
    }
    return super.onOptionsItemSelected(item)
  }

  private fun readIntentExtras() {
    refName = intent.extras?.getString(EXTRA_REF_NAME)
    refLabel = intent.extras?.getString(EXTRA_REF_LABEL)
    refType = intent.extras?.getInt(EXTRA_REF_TYPE) ?: NATIVE
    refList = intent.extras?.getStringArray(EXTRA_REF_LIST)
  }

  private fun updateActionBar() {
    supportActionBar?.apply {
      refLabel?.let { title = it }
      subtitle = refName
    }
  }

  private fun initView() {
    setSupportActionBar(binding.toolbar)
    updateActionBar()
    binding.apply {
      supportActionBar?.setDisplayHomeAsUpEnabled(true)
      (root as ViewGroup).bringChildToFront(appbar)

      list.apply {
        adapter = this@LibReferenceActivity.adapter
        applySystemBarsPadding(top = true, bottom = true)
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
      loading.setRuleIconHighlightProvider()
    }

    viewModel.libRefListFlow.onEach {
      val itemViewStates = viewModel.buildAppListItemViewStates(it)
      adapter.bind(AppListRenderState(itemViewStates = itemViewStates))
      adapter.setList(it)
      if (binding.vfContainer.displayedChild != 1) {
        binding.vfContainer.displayedChild = 1
      }
    }.launchIn(lifecycleScope)

    adapter.setOnItemClickListener { _, view, position ->
      if (AntiShakeUtils.isInvalidClick(view)) {
        return@setOnItemClickListener
      }
      val item = adapter.getItem(position)
      val (name, type) = if (refType == ACTION) {
        val pair = viewModel.getActionTarget(item.packageName)
        (pair?.first ?: item.packageName) to (pair?.second ?: ACTION)
      } else if (refList != null && refType == PROVIDER) {
        val providerName = refList
          ?.find { it.startsWith(item.packageName + "|") }
          ?.substringAfter("|")
        (providerName ?: refName) to refType
      } else {
        refName to refType
      }

      launchDetailPage(item = item, refName = name, refType = type)
    }
  }

  private fun initData() {
    this.refName?.let { name ->
      refList?.toList()?.let { encodedList ->
        val pkgNames = encodedList.map { it.substringBefore("|") }
        viewModel.setData(name, refType, pkgNames)
      } ?: viewModel.setData(name, refType)
    } ?: finish()
  }
}
