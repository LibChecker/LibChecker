package com.absinthe.libchecker.features.album.comparison.ui

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
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
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.databinding.ActivityComparisonBinding
import com.absinthe.libchecker.domain.app.GetRandomAppIconUseCase
import com.absinthe.libchecker.domain.snapshot.ArchiveSnapshotItem
import com.absinthe.libchecker.domain.snapshot.BuildArchiveSnapshotItemUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotAbiDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotComparisonPlan
import com.absinthe.libchecker.features.album.comparison.ui.view.ComparisonDashboardView
import com.absinthe.libchecker.features.snapshot.SnapshotViewModel
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
import java.io.File
import kotlinx.coroutines.CancellationException
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

  private val viewModel: SnapshotViewModel by viewModel()
  private val getRandomAppIcon: GetRandomAppIconUseCase by inject()
  private val buildSnapshotAbiDisplayData: BuildSnapshotAbiDisplayDataUseCase by inject()
  private val adapter by lazy(LazyThreadSafetyMode.NONE) {
    SnapshotAdapter(buildSnapshotAbiDisplayData)
  }
  private var leftTimeStamp = 0L
  private var rightTimeStamp = 0L
  private var isLeftPartChoosing = false
  private var leftUri: Uri? = null
  private var rightUri: Uri? = null

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
    if (leftTimeStamp == -1L || rightTimeStamp == -1L) {
      externalCacheDir?.let { cacheDir ->
        File(cacheDir, Constants.TEMP_PACKAGE).delete()
        File(cacheDir, Constants.TEMP_PACKAGE_2).delete()
        File(cacheDir, "apks").deleteRecursively()
      }
    }
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.comparison_menu, menu)
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    if (menuItem.itemId == R.id.compare) {
      if (binding.vfContainer.displayedChild == VF_LOADING) {
        return false
      }
      if (leftTimeStamp == 0L || rightTimeStamp == 0L) {
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
      registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (isLeftPartChoosing) {
          leftTimeStamp = -1
          leftUri = it
        } else {
          rightTimeStamp = -1
          rightUri = it
        }
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
          lifecycleScope.launch(Dispatchers.IO) {
            val timeStampList = viewModel.getTimeStamps()
            val dialog = TimeNodeBottomSheetDialogFragment
              .newInstance(ArrayList(timeStampList))
              .apply {
                setCompareMode(true)
                setLeftMode(true)
                setOnItemClickListener { position ->
                  val item = timeStampList[position]
                  leftTimeStamp = item.timestamp
                  invalidateDashboard()
                  dismiss()
                }
              }
            dialog.show(supportFragmentManager, TimeNodeBottomSheetDialogFragment::class.java.name)
          }
        }
      }
      container.rightPart.apply {
        setOnClickListener {
          if (AntiShakeUtils.isInvalidClick(it)) {
            return@setOnClickListener
          }
          lifecycleScope.launch(Dispatchers.IO) {
            val timeStampList = viewModel.getTimeStamps()
            val dialog = TimeNodeBottomSheetDialogFragment
              .newInstance(ArrayList(timeStampList))
              .apply {
                setCompareMode(true)
                setLeftMode(false)
                setOnItemClickListener { position ->
                  val item = timeStampList[position]
                  rightTimeStamp = item.timestamp
                  invalidateDashboard()
                  dismiss()
                }
              }
            dialog.show(supportFragmentManager, TimeNodeBottomSheetDialogFragment::class.java.name)
          }
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
          is SnapshotViewModel.Effect.ChooseComparedApk -> {
            isLeftPartChoosing = it.isLeftPart
            chooseApkResultLauncher.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream"))
          }

          is SnapshotViewModel.Effect.DashboardCountChange -> {
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

          is SnapshotViewModel.Effect.DiffItemChange -> {
            val newItems = adapter.data
              .asSequence()
              .map { i ->
                if (i.packageName == it.item.packageName) {
                  it.item
                } else {
                  i
                }
              }
              .toList()
            adapter.setList(newItems.sortedByDescending { item -> item.updateTime })
            flip(VF_LIST)
          }

          else -> {}
        }
      }.launchIn(lifecycleScope)
    }
  }

  private fun parseIntent(intent: Intent) {
    when (val result = ComparisonShareIntentParser.parse(intent)) {
      ComparisonShareIntentParser.Result.None -> Unit

      ComparisonShareIntentParser.Result.InvalidSharedItems -> {
        showToast(R.string.album_item_comparison_invalid_shared_items)
      }

      is ComparisonShareIntentParser.Result.PackagePair -> {
        result.leftUri?.let {
          leftTimeStamp = -1
          leftUri = it
        }
        result.rightUri?.let {
          rightTimeStamp = -1
          rightUri = it
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
        if (leftTimeStamp == -1L) {
          leftUri?.encodedPath?.let { path ->
            it.leftPart.tvSnapshotTimestampText.text = getUriFileName(path)
          }
          it.leftPart.tvSnapshotAppsCountText.text = 1.toString()
        } else if (leftTimeStamp > 0) {
          it.leftPart.tvSnapshotTimestampText.text = viewModel.getFormatDateString(leftTimeStamp)
          viewModel.getDashboardCount(leftTimeStamp, true)
        }
        it.leftPart.updateContentDescription()

        if (rightTimeStamp == -1L) {
          rightUri?.encodedPath?.let { path ->
            it.rightPart.tvSnapshotTimestampText.text = getUriFileName(path)
          }
          it.rightPart.tvSnapshotAppsCountText.text = 1.toString()
        } else if (rightTimeStamp > 0) {
          it.rightPart.tvSnapshotTimestampText.text = viewModel.getFormatDateString(rightTimeStamp)
          viewModel.getDashboardCount(rightTimeStamp, false)
        }
        it.rightPart.updateContentDescription()
      }
    }
  }

  private fun getUriFileName(path: String): String {
    val decode = Uri.decode(path)
    return if (decode.contains(File.separator)) {
      getUriFileName(decode.split(File.separator).last())
    } else {
      decode
    }
  }

  private fun compareSelectedItems() = lifecycleScope.launch(Dispatchers.IO) {
    val hasArchiveInput = leftTimeStamp == -1L || rightTimeStamp == -1L
    var dialog: AlertDialog? = null
    if (hasArchiveInput) {
      withContext(Dispatchers.Main) {
        dialog = UiUtils.createLoadingDialog(this@ComparisonActivity).also {
          it.show()
        }
      }
    }

    val leftArchive = buildSelectedArchiveSnapshotItem(
      timeStamp = leftTimeStamp,
      uri = leftUri,
      fileName = Constants.TEMP_PACKAGE
    )
    val rightArchive = buildSelectedArchiveSnapshotItem(
      timeStamp = rightTimeStamp,
      uri = rightUri,
      fileName = Constants.TEMP_PACKAGE_2
    )

    withContext(Dispatchers.Main) {
      dialog?.dismiss()
    }

    when (
      val plan = viewModel.buildSnapshotComparisonPlan(
        leftTimeStamp = leftTimeStamp,
        leftArchive = leftArchive,
        rightTimeStamp = rightTimeStamp,
        rightArchive = rightArchive
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
        viewModel.compareDiffWithSnapshotList(-1, plan.lists.left, plan.lists.right)
      }
    }
  }

  private suspend fun buildSelectedArchiveSnapshotItem(
    timeStamp: Long,
    uri: Uri?,
    fileName: String
  ): ArchiveSnapshotItem? {
    return runCatching {
      if (timeStamp == -1L && uri != null) {
        getArchiveSnapshotItemByUri(uri, fileName)
      } else {
        null
      }
    }.getOrElse {
      handleArchiveSnapshotFailure(it)
      null
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

  private suspend fun getArchiveSnapshotItemByUri(
    uri: Uri,
    fileName: String
  ): ArchiveSnapshotItem {
    val iconSize = resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size)
    return viewModel.buildArchiveSnapshotItem(
      uri = uri,
      destinationFile = File(requireAvailableCacheDir(), fileName),
      iconSize = iconSize
    )
  }

  private suspend fun handleArchiveSnapshotFailure(throwable: Throwable) {
    if (throwable is CancellationException) {
      throw throwable
    }
    if (throwable is BuildArchiveSnapshotItemUseCase.NotEnoughStorageSpaceException) {
      withContext(Dispatchers.Main) {
        showToast(R.string.toast_not_enough_storage_space)
      }
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
