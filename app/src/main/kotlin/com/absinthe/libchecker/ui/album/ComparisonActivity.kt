package com.absinthe.libchecker.ui.album

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.ActivityComparisonBinding
import com.absinthe.libchecker.recyclerview.HorizontalSpacesItemDecoration
import com.absinthe.libchecker.recyclerview.adapter.snapshot.SnapshotAdapter
import com.absinthe.libchecker.ui.detail.EXTRA_ENTITY
import com.absinthe.libchecker.ui.detail.SnapshotDetailActivity
import com.absinthe.libchecker.ui.fragment.snapshot.TimeNodeBottomSheetDialogFragment
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.snapshot.ComparisonDashboardView
import com.absinthe.libchecker.view.snapshot.SnapshotEmptyView
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.widget.borderview.BorderView

const val VF_LOADING = 0
const val VF_LIST = 1

class ComparisonActivity : BaseActivity<ActivityComparisonBinding>() {

  private val viewModel: SnapshotViewModel by viewModels()
  private val adapter by unsafeLazy { SnapshotAdapter(lifecycleScope) }
  private var leftTimeStamp = 0L
  private var rightTimeStamp = 0L

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    initView()
  }

  override fun onDestroy() {
    super.onDestroy()
    adapter.release()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressed()
    }
    return super.onOptionsItemSelected(item)
  }

  private fun initView() {
    setAppBar(binding.appbar, binding.toolbar)
    (binding.root as ViewGroup).bringChildToFront(binding.appbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    binding.toolbar.title = getString(R.string.album_item_comparison_title)

    val dashboardView = ComparisonDashboardView(
      ContextThemeWrapper(this, R.style.AlbumMaterialCard)
    )

    dashboardView.apply {
      layoutParams = ViewGroup.MarginLayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      if (!GlobalValues.md3Theme) {
        background = null
      }
      container.leftPart.apply {
        setOnClickListener {
          if (AntiShakeUtils.isInvalidClick(it)) {
            return@setOnClickListener
          }
          lifecycleScope.launch(Dispatchers.IO) {
            val timeStampList = viewModel.repository.getTimeStamps()
            val dialog = TimeNodeBottomSheetDialogFragment
              .newInstance(ArrayList(timeStampList))
              .apply {
                setOnItemClickListener { position ->
                  val item = timeStampList[position]
                  leftTimeStamp = item.timestamp
                  tvSnapshotTimestampText.text = viewModel.getFormatDateString(leftTimeStamp)

                  this@ComparisonActivity.lifecycleScope.launch {
                    val count = viewModel.repository.getSnapshots(leftTimeStamp).size
                    tvSnapshotAppsCountText.text = count.toString()
                  }
                  dismiss()
                }
              }
            dialog.show(supportFragmentManager, dialog.tag)
          }
        }
      }
      container.rightPart.apply {
        setOnClickListener {
          if (AntiShakeUtils.isInvalidClick(it)) {
            return@setOnClickListener
          }
          lifecycleScope.launch(Dispatchers.IO) {
            val timeStampList = viewModel.repository.getTimeStamps()
            val dialog = TimeNodeBottomSheetDialogFragment
              .newInstance(ArrayList(timeStampList))
              .apply {
                setOnItemClickListener { position ->
                  val item = timeStampList[position]
                  rightTimeStamp = item.timestamp
                  tvSnapshotTimestampText.text = viewModel.getFormatDateString(rightTimeStamp)
                  this@ComparisonActivity.lifecycleScope.launch {
                    val count = viewModel.repository.getSnapshots(rightTimeStamp).size
                    tvSnapshotAppsCountText.text = count.toString()
                  }
                  dismiss()
                }
              }
            dialog.show(supportFragmentManager, dialog.tag)
          }
        }
      }
    }

    binding.apply {
      extendedFab.apply {
        setOnClickListener {
          if (AntiShakeUtils.isInvalidClick(it)) {
            return@setOnClickListener
          }
          if (leftTimeStamp == rightTimeStamp || leftTimeStamp == 0L || rightTimeStamp == 0L) {
            Toasty.showShort(
              this@ComparisonActivity,
              R.string.album_item_comparison_invalid_compare
            )
            return@setOnClickListener
          }
          viewModel.compareDiff(
            leftTimeStamp.coerceAtMost(rightTimeStamp),
            leftTimeStamp.coerceAtLeast(rightTimeStamp)
          )
          flip(VF_LOADING)
        }
        setOnLongClickListener {
          if (adapter.data.isNotEmpty()) {
            hide()
          }
          true
        }
      }
      recyclerview.apply {
        adapter = this@ComparisonActivity.adapter
        layoutManager = getSuitableLayoutManager()
        borderVisibilityChangedListener =
          BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
            appBar?.setRaised(!top)
          }

        if (itemDecorationCount == 0) {
          addItemDecoration(
            HorizontalSpacesItemDecoration(
              resources.getDimension(R.dimen.normal_padding).toInt() / 2
            )
          )
        }
      }
      vfContainer.apply {
        setInAnimation(this@ComparisonActivity, R.anim.anim_fade_in)
        setOutAnimation(this@ComparisonActivity, R.anim.anim_fade_out)
        displayedChild = VF_LIST
      }
    }

    adapter.apply {
      headerWithEmptyEnable = true
      val emptyView = SnapshotEmptyView(this@ComparisonActivity).apply {
        layoutParams = FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.MATCH_PARENT,
          FrameLayout.LayoutParams.WRAP_CONTENT
        ).also {
          it.gravity = Gravity.CENTER_HORIZONTAL
        }
        addPaddingTop(96.dp)
      }
      setEmptyView(emptyView)
      setHeaderView(dashboardView)
      setOnItemClickListener { _, view, position ->
        if (AntiShakeUtils.isInvalidClick(view)) {
          return@setOnItemClickListener
        }

        val intent = Intent(this@ComparisonActivity, SnapshotDetailActivity::class.java)
          .putExtras(bundleOf(EXTRA_ENTITY to getItem(position)))

        startActivity(intent)
      }
    }

    viewModel.snapshotDiffItems.observe(this) { list ->
      adapter.setList(list.sortedByDescending { it.updateTime })
      flip(VF_LIST)
    }
  }

  private fun getSuitableLayoutManager(): RecyclerView.LayoutManager {
    return when (resources.configuration.orientation) {
      Configuration.ORIENTATION_PORTRAIT -> LinearLayoutManager(this)
      Configuration.ORIENTATION_LANDSCAPE -> StaggeredGridLayoutManager(
        2,
        StaggeredGridLayoutManager.VERTICAL
      )
      else -> throw IllegalStateException("Wrong orientation at AppListFragment.")
    }
  }

  private fun flip(child: Int) {
    if (binding.vfContainer.displayedChild == child) {
      return
    }
    if (child == VF_LOADING) {
      if (binding.extendedFab.isShown) {
        binding.extendedFab.hide()
      }
      binding.loading.resumeAnimation()
    } else {
      if (!binding.extendedFab.isShown) {
        binding.extendedFab.show()
      }
      binding.loading.pauseAnimation()
    }

    binding.vfContainer.displayedChild = child
  }
}
