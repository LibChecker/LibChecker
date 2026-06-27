package com.absinthe.libchecker.features.album.comparison.ui

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.createBitmap
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.databinding.ActivityComparisonBinding
import com.absinthe.libchecker.domain.app.GetRandomAppIconUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotAbiDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotComparisonPlan
import com.absinthe.libchecker.domain.snapshot.comparison.SnapshotComparisonSide
import com.absinthe.libchecker.domain.snapshot.display.BuildSnapshotUpdateTimeDisplayDataUseCase
import com.absinthe.libchecker.features.album.comparison.SnapshotComparisonViewModel
import com.absinthe.libchecker.features.album.comparison.ui.view.ComparisonDashboardHalfView
import com.absinthe.libchecker.features.album.comparison.ui.view.ComparisonDashboardView
import com.absinthe.libchecker.features.snapshot.detail.ui.EXTRA_ENTITY
import com.absinthe.libchecker.features.snapshot.detail.ui.EXTRA_ICON
import com.absinthe.libchecker.features.snapshot.detail.ui.SnapshotDetailActivity
import com.absinthe.libchecker.features.snapshot.detail.ui.view.SnapshotEmptyView
import com.absinthe.libchecker.features.snapshot.ui.TimeNodeBottomSheetDialogFragment
import com.absinthe.libchecker.features.snapshot.ui.VF_LIST
import com.absinthe.libchecker.features.snapshot.ui.adapter.SnapshotAdapter
import com.absinthe.libchecker.ui.adapter.HorizontalSpacesItemDecoration
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.applySystemBarsPadding
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.requireAvailableCacheDir
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import rikka.widget.borderview.BorderView

const val VF_LOADING = 0
const val VF_LIST = 1

class ComparisonActivity :
  BaseActivity<ActivityComparisonBinding>(),
  MenuProvider {

  private val viewModel: SnapshotComparisonViewModel by viewModel()
  private val getRandomAppIcon: GetRandomAppIconUseCase by inject()
  private val buildSnapshotAbiDisplayData: BuildSnapshotAbiDisplayDataUseCase by inject()
  private val buildSnapshotUpdateTimeDisplayData: BuildSnapshotUpdateTimeDisplayDataUseCase by inject()
  private val adapter by lazy(LazyThreadSafetyMode.NONE) {
    SnapshotAdapter(buildSnapshotAbiDisplayData, buildSnapshotUpdateTimeDisplayData)
  }
  private var archiveChoosingSide = SnapshotComparisonSide.LEFT

  private lateinit var chooseApkResultLauncher: ActivityResultLauncher<Array<String>>

  private val dashboardView by lazy {
    ComparisonDashboardView(
      ContextThemeWrapper(this, R.style.AlbumMaterialCard)
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initView()
    registerCallbacks()
    parseIntent(intent)
  }

  override fun onDestroy() {
    super.onDestroy()
    viewModel.clearSnapshotComparisonArchiveCache(externalCacheDir)
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.comparison_menu, menu)
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    if (menuItem.itemId == R.id.compare) {
      if (binding.vfContainer.displayedChild == VF_LOADING) {
        return false
      }
      if (!viewModel.inputs.canCompare) {
        showToast(R.string.album_item_comparison_invalid_compare)
        return false
      }
      compareSelectedItems()
    }
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressedDispatcher.onBackPressed()
    }
    return super.onOptionsItemSelected(item)
  }

  private fun registerCallbacks() {
    chooseApkResultLauncher =
      registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
          return@registerForActivityResult
        }
        viewModel.selectArchive(archiveChoosingSide, uri)
        invalidateDashboard()
      }
  }

  private fun initView() {
    addMenuProvider(this, this, Lifecycle.State.CREATED)
    setSupportActionBar(binding.toolbar)
    (binding.root as ViewGroup).bringChildToFront(binding.appbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    binding.toolbar.title = getString(R.string.album_item_comparison_title)

    dashboardView.apply {
      layoutParams = ViewGroup.MarginLayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      container.leftPart.apply {
        setOnClickListener {
          if (AntiShakeUtils.isInvalidClick(it)) {
            return@setOnClickListener
          }
          showTimeNodePicker(SnapshotComparisonSide.LEFT)
        }
      }
      container.rightPart.apply {
        setOnClickListener {
          if (AntiShakeUtils.isInvalidClick(it)) {
            return@setOnClickListener
          }
          showTimeNodePicker(SnapshotComparisonSide.RIGHT)
        }
      }
    }

    binding.apply {
      recyclerview.apply {
        adapter = this@ComparisonActivity.adapter
        applySystemBarsPadding(top = true, bottom = true)
        layoutManager = getSuitableLayoutManager()
        borderVisibilityChangedListener =
          BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
            appbar.isLifted = !top
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
      loading.setAppIconHighlightProvider { getRandomAppIcon() }
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
      stateView = emptyView
      isStateViewEnable = true
      setHeaderView(dashboardView)
      setOnItemClickListener { _, view, position ->
        if (AntiShakeUtils.isInvalidClick(view)) {
          return@setOnItemClickListener
        }

        val intent = Intent(this@ComparisonActivity, SnapshotDetailActivity::class.java)
          .putExtras(
            Bundle().apply {
              putSerializable(EXTRA_ENTITY, getItem(position))
            }
          )

        startActivity(intent)
      }
    }

    viewModel.apply {
      snapshotDiffItemsFlow.onEach {
        adapter.setList(it.sortedByDescending { item -> item.updateTime })
        flip(VF_LIST)
      }.launchIn(lifecycleScope)
      effect.onEach {
        when (it) {
          is SnapshotComparisonViewModel.Effect.DashboardCountChange -> {
            if (it.isLeft) {
              dashboardView.container.leftPart.tvSnapshotAppsCountText.text =
                it.snapshotCount.toString()
              dashboardView.container.leftPart.updateContentDescription()
            } else {
              dashboardView.container.rightPart.tvSnapshotAppsCountText.text =
                it.snapshotCount.toString()
              dashboardView.container.rightPart.updateContentDescription()
            }
          }
        }
      }.launchIn(lifecycleScope)
    }
  }

  private fun showTimeNodePicker(side: SnapshotComparisonSide) {
    lifecycleScope.launch(Dispatchers.IO) {
      val timeStampList = viewModel.getTimeStamps()
      withContext(Dispatchers.Main) {
        TimeNodeBottomSheetDialogFragment
          .newInstance(ArrayList(timeStampList))
          .apply {
            setCompareMode(true)
            setLeftMode(side == SnapshotComparisonSide.LEFT)
            setOnAddApkClickListener(::chooseApk)
            setOnItemClickListener { position ->
              val item = timeStampList[position]
              viewModel.selectSnapshot(side, item.timestamp)
              invalidateDashboard()
              dismiss()
            }
          }
          .show(supportFragmentManager, TimeNodeBottomSheetDialogFragment::class.java.name)
      }
    }
  }

  private fun chooseApk(isLeft: Boolean) {
    archiveChoosingSide = SnapshotComparisonSide.fromIsLeft(isLeft)
    chooseApkResultLauncher.launch(
      arrayOf("application/vnd.android.package-archive", "application/octet-stream")
    )
  }

  private fun parseIntent(intent: Intent) {
    when (val result = ComparisonShareIntentParser.parse(intent)) {
      ComparisonShareIntentParser.Result.None -> Unit

      ComparisonShareIntentParser.Result.InvalidSharedItems -> {
        showToast(R.string.album_item_comparison_invalid_shared_items)
      }

      is ComparisonShareIntentParser.Result.PackagePair -> {
        result.leftUri?.let {
          viewModel.selectArchive(SnapshotComparisonSide.LEFT, it)
        }
        result.rightUri?.let {
          viewModel.selectArchive(SnapshotComparisonSide.RIGHT, it)
        }
        repeat(result.invalidItemCount) {
          showToast(R.string.album_item_comparison_invalid_shared_items)
        }
        invalidateDashboard()
        compareSelectedItems()
      }
    }
  }

  private fun invalidateDashboard() {
    dashboardView.container.let {
      it.post {
        it.leftPart.applyDashboardSideState(
          ComparisonDashboardStatePlanner.planSideState(
            input = viewModel.inputs.left,
            formatTimestamp = viewModel::getFormatDateString
          ),
          isLeft = true
        )
        it.rightPart.applyDashboardSideState(
          ComparisonDashboardStatePlanner.planSideState(
            input = viewModel.inputs.right,
            formatTimestamp = viewModel::getFormatDateString
          ),
          isLeft = false
        )
      }
    }
  }

  private fun ComparisonDashboardHalfView.applyDashboardSideState(
    sideState: ComparisonDashboardStatePlanner.SideState,
    isLeft: Boolean
  ) {
    sideState.timestampText?.let { tvSnapshotTimestampText.text = it }
    sideState.appsCountText?.let { tvSnapshotAppsCountText.text = it }
    sideState.dashboardCountTimestamp?.let { viewModel.getDashboardCount(it, isLeft) }
    updateContentDescription()
  }

  private fun compareSelectedItems() = lifecycleScope.launch(Dispatchers.IO) {
    val inputs = viewModel.inputs
    var dialog: AlertDialog? = null
    if (inputs.hasArchiveInput) {
      withContext(Dispatchers.Main) {
        dialog = UiUtils.createLoadingDialog(this@ComparisonActivity).also {
          it.show()
        }
      }
    }

    val archiveResult = if (inputs.hasArchiveInput) {
      viewModel.prepareSnapshotComparisonArchives(
        cacheDir = requireAvailableCacheDir(),
        iconSize = resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size)
      )
    } else {
      null
    }

    withContext(Dispatchers.Main) {
      dialog?.dismiss()
      if (archiveResult?.hasNotEnoughStorageSpace == true) {
        showToast(R.string.toast_not_enough_storage_space)
      }
    }

    when (
      val plan = viewModel.buildSnapshotComparisonPlan(
        inputs = inputs,
        leftArchive = archiveResult?.leftArchive,
        rightArchive = archiveResult?.rightArchive
      )
    ) {
      null -> {
        withContext(Dispatchers.Main) {
          showToast(R.string.album_item_comparison_invalid_compare)
        }
      }

      is SnapshotComparisonPlan.TimestampRange -> {
        withContext(Dispatchers.Main) {
          viewModel.compareDiff(plan.previousTimestamp, plan.currentTimestamp)
          flip(VF_LOADING)
        }
      }

      is SnapshotComparisonPlan.ArchivePair -> {
        showArchiveComparison(plan)
      }

      is SnapshotComparisonPlan.SnapshotLists -> {
        flip(VF_LOADING)
        viewModel.compareDiffWithSnapshotList(plan.lists.left, plan.lists.right)
      }
    }
  }

  private suspend fun showArchiveComparison(plan: SnapshotComparisonPlan.ArchivePair) {
    val leftPackage = plan.left.snapshotItem
    val rightPackage = plan.right.snapshotItem
    val leftIcon = plan.left.icon
    val rightIcon = plan.right.icon

    withContext(Dispatchers.Main) {
      if (plan.requiresDifferentPackageConfirmation) {
        BaseAlertDialogBuilder(this@ComparisonActivity)
          .setTitle(R.string.dialog_title_compare_diff_apk)
          .setMessage(R.string.dialog_message_compare_diff_apk)
          .setPositiveButton(android.R.string.ok) { _, _ ->
            navigateToSnapshotDetail(
              leftPackage,
              rightPackage,
              leftIcon,
              rightIcon
            )
          }
          .setNegativeButton(android.R.string.cancel, null)
          .show()
      } else {
        navigateToSnapshotDetail(leftPackage, rightPackage, leftIcon, rightIcon)
      }
    }
  }

  private fun navigateToSnapshotDetail(
    left: SnapshotItem,
    right: SnapshotItem,
    leftIcon: Bitmap,
    rightIcon: Bitmap
  ) {
    val intent = Intent(this, SnapshotDetailActivity::class.java)
      .putExtras(
        Bundle().apply {
          putSerializable(EXTRA_ENTITY, viewModel.buildSnapshotPairDiff(left, right))
          putParcelable(EXTRA_ICON, getIconsCombo(leftIcon, rightIcon))
        }
      )
    startActivity(intent)
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

  private fun flip(child: Int) = lifecycleScope.launch(Dispatchers.Main) {
    if (binding.vfContainer.displayedChild == child) {
      return@launch
    }
    if (child == VF_LOADING) {
      binding.loading.start()
    } else {
      binding.loading.stop()
    }

    binding.vfContainer.displayedChild = child
  }

  private fun getIconsCombo(leftIconOrigin: Bitmap, rightIconOrigin: Bitmap): Bitmap {
    val iconSize = resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size)
    val leftIcon = Bitmap.createBitmap(leftIconOrigin, 0, 0, leftIconOrigin.width / 2, leftIconOrigin.height)
    val rightIcon = Bitmap.createBitmap(rightIconOrigin, rightIconOrigin.width / 2, 0, rightIconOrigin.width / 2, rightIconOrigin.height)
    val comboIcon = createBitmap(iconSize, iconSize)
    val isSameIcon = leftIconOrigin.sameAs(rightIconOrigin)

    Canvas(comboIcon).apply {
      drawBitmap(leftIcon, 0f, 0f, null)
      drawBitmap(rightIcon, iconSize / 2f, 0f, null)
      if (!isSameIcon) {
        drawLine(
          iconSize / 2f,
          0f,
          iconSize / 2f,
          iconSize.toFloat(),
          Paint().apply {
            color = getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
            strokeWidth = 2.dp.toFloat()
          }
        )
      }
      save()
      restore()
    }
    return comboIcon
  }
}
