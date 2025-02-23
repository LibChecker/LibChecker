package com.absinthe.libchecker.features.album.comparison.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.createBitmap
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.databinding.ActivityComparisonBinding
import com.absinthe.libchecker.features.album.comparison.ui.view.ComparisonDashboardView
import com.absinthe.libchecker.features.snapshot.SnapshotViewModel
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem
import com.absinthe.libchecker.features.snapshot.detail.ui.EXTRA_ENTITY
import com.absinthe.libchecker.features.snapshot.detail.ui.EXTRA_ICON
import com.absinthe.libchecker.features.snapshot.detail.ui.SnapshotDetailActivity
import com.absinthe.libchecker.features.snapshot.detail.ui.view.SnapshotEmptyView
import com.absinthe.libchecker.features.snapshot.ui.TimeNodeBottomSheetDialogFragment
import com.absinthe.libchecker.features.snapshot.ui.adapter.SnapshotAdapter
import com.absinthe.libchecker.ui.adapter.HorizontalSpacesItemDecoration
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersion
import com.absinthe.libchecker.utils.extensions.getPackageSize
import com.absinthe.libchecker.utils.extensions.getPermissionsList
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libchecker.utils.toJson
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader
import okio.buffer
import okio.sink
import okio.source
import rikka.widget.borderview.BorderView

const val VF_LOADING = 0
const val VF_LIST = 1

class ComparisonActivity : BaseActivity<ActivityComparisonBinding>() {

  private val viewModel: SnapshotViewModel by viewModels()
  private val adapter = SnapshotAdapter()
  private var leftTimeStamp = 0L
  private var rightTimeStamp = 0L
  private var isLeftPartChoosing = false
  private var leftUri: Uri? = null
  private var rightUri: Uri? = null
  private var leftIconOriginal: Bitmap? = null
  private var rightIconOriginal: Bitmap? = null

  private lateinit var chooseApkResultLauncher: ActivityResultLauncher<String>

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
      externalCacheDir?.deleteRecursively()
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressedDispatcher.onBackPressed()
    }
    return super.onOptionsItemSelected(item)
  }

  private fun registerCallbacks() {
    chooseApkResultLauncher =
      registerForActivityResult(ActivityResultContracts.GetContent()) {
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
            val timeStampList = viewModel.repository.getTimeStamps()
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
            val timeStampList = viewModel.repository.getTimeStamps()
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
      extendedFab.apply {
        setOnClickListener {
          if (AntiShakeUtils.isInvalidClick(it)) {
            return@setOnClickListener
          }
          if (leftTimeStamp == 0L || rightTimeStamp == 0L) {
            showToast(R.string.album_item_comparison_invalid_compare)
            return@setOnClickListener
          }
          if (leftTimeStamp != -1L && rightTimeStamp != -1L) {
            if (leftTimeStamp == rightTimeStamp) {
              showToast(R.string.album_item_comparison_invalid_compare)
              return@setOnClickListener
            }
            viewModel.compareDiff(
              leftTimeStamp.coerceAtMost(rightTimeStamp),
              leftTimeStamp.coerceAtLeast(rightTimeStamp)
            )
            flip(VF_LOADING)
          } else {
            compareDiffContainsApk()
          }
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

    viewModel.apply {
      snapshotDiffItemsFlow.onEach {
        adapter.setList(it.sortedByDescending { item -> item.updateTime })
        flip(VF_LIST)
      }.launchIn(lifecycleScope)
      effect.onEach {
        when (it) {
          is SnapshotViewModel.Effect.ChooseComparedApk -> {
            isLeftPartChoosing = it.isLeftPart
            chooseApkResultLauncher.launch("application/vnd.android.package-archive")
          }

          is SnapshotViewModel.Effect.DashboardCountChange -> {
            if (it.isLeft) {
              dashboardView.container.leftPart.tvSnapshotAppsCountText.text =
                it.snapshotCount.toString()
            } else {
              dashboardView.container.rightPart.tvSnapshotAppsCountText.text =
                it.snapshotCount.toString()
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
    if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
      intent.extras?.let {
        val uriList = BundleCompat.getParcelableArrayList(it, Intent.EXTRA_STREAM, Uri::class.java)
        if (uriList?.size == 2) {
          if (uriList[0].encodedPath?.endsWith(".apk") == true) {
            leftTimeStamp = -1
            leftUri = uriList[0]
          } else {
            showToast(R.string.album_item_comparison_invalid_shared_items)
          }

          if (uriList[1].encodedPath?.endsWith(".apk") == true) {
            rightTimeStamp = -1
            rightUri = uriList[1]
          } else {
            showToast(R.string.album_item_comparison_invalid_shared_items)
          }

          invalidateDashboard()
          compareDiffContainsApk()
        } else {
          showToast(R.string.album_item_comparison_invalid_shared_items)
        }
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

        if (rightTimeStamp == -1L) {
          rightUri?.encodedPath?.let { path ->
            it.rightPart.tvSnapshotTimestampText.text = getUriFileName(path)
          }
          it.rightPart.tvSnapshotAppsCountText.text = 1.toString()
        } else if (rightTimeStamp > 0) {
          it.rightPart.tvSnapshotTimestampText.text = viewModel.getFormatDateString(rightTimeStamp)
          viewModel.getDashboardCount(rightTimeStamp, false)
        }
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

  private fun compareDiffContainsApk() = lifecycleScope.launch(Dispatchers.IO) {
    var dialog: AlertDialog?
    withContext(Dispatchers.Main) {
      dialog = UiUtils.createLoadingDialog(this@ComparisonActivity).also {
        it.show()
      }
    }
    val leftPackage = runCatching {
      if (leftTimeStamp == -1L && leftUri != null) {
        getSnapshotItemByUri(leftUri!!, Constants.TEMP_PACKAGE)
      } else {
        null
      }
    }.getOrNull()
    val rightPackage = runCatching {
      if (rightTimeStamp == -1L && rightUri != null) {
        getSnapshotItemByUri(rightUri!!, Constants.TEMP_PACKAGE_2)
      } else {
        null
      }
    }.getOrNull()

    withContext(Dispatchers.Main) {
      dialog?.dismiss()
    }

    if (leftPackage != null && rightPackage != null) {
      if (leftPackage.packageName != rightPackage.packageName) {
        withContext(Dispatchers.Main) {
          BaseAlertDialogBuilder(this@ComparisonActivity)
            .setTitle(R.string.dialog_title_compare_diff_apk)
            .setMessage(R.string.dialog_message_compare_diff_apk)
            .setPositiveButton(android.R.string.ok) { _, _ ->
              navigateToSnapshotDetail(
                leftPackage,
                rightPackage
              )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        }
      } else {
        navigateToSnapshotDetail(leftPackage, rightPackage)
      }
      return@launch
    }
    val leftSnapshots: List<SnapshotItem> = if (leftPackage != null) {
      listOf(leftPackage)
    } else if (leftTimeStamp > 0) {
      if (rightPackage != null) {
        viewModel.repository.getSnapshots(leftTimeStamp)
          .filter { it.packageName == rightPackage.packageName }
      } else {
        viewModel.repository.getSnapshots(leftTimeStamp)
      }
    } else {
      showToast(R.string.album_item_comparison_invalid_compare)
      return@launch
    }

    val rightSnapshots: List<SnapshotItem> = if (rightPackage != null) {
      listOf(rightPackage)
    } else if (rightTimeStamp > 0) {
      if (leftPackage != null) {
        viewModel.repository.getSnapshots(rightTimeStamp)
          .filter { it.packageName == leftPackage.packageName }
      } else {
        viewModel.repository.getSnapshots(rightTimeStamp)
      }
    } else {
      showToast(R.string.album_item_comparison_invalid_compare)
      return@launch
    }

    flip(VF_LOADING)
    viewModel.compareDiffWithSnapshotList(-1, leftSnapshots, rightSnapshots)
  }

  private fun navigateToSnapshotDetail(left: SnapshotItem, right: SnapshotItem) {
    val snapshotDiff = SnapshotDiffItem(
      packageName = "${left.packageName}/${right.packageName}",
      updateTime = -1,
      labelDiff = SnapshotDiffItem.DiffNode(left.label, right.label),
      versionNameDiff = SnapshotDiffItem.DiffNode(left.versionName, right.versionName),
      versionCodeDiff = SnapshotDiffItem.DiffNode(left.versionCode, right.versionCode),
      abiDiff = SnapshotDiffItem.DiffNode(left.abi, right.abi),
      targetApiDiff = SnapshotDiffItem.DiffNode(left.targetApi, right.targetApi),
      compileSdkDiff = SnapshotDiffItem.DiffNode(left.compileSdk, right.compileSdk),
      minSdkDiff = SnapshotDiffItem.DiffNode(left.minSdk, right.minSdk),
      nativeLibsDiff = SnapshotDiffItem.DiffNode(left.nativeLibs, right.nativeLibs),
      servicesDiff = SnapshotDiffItem.DiffNode(left.services, right.services),
      activitiesDiff = SnapshotDiffItem.DiffNode(left.activities, right.activities),
      receiversDiff = SnapshotDiffItem.DiffNode(left.receivers, right.receivers),
      providersDiff = SnapshotDiffItem.DiffNode(left.providers, right.providers),
      permissionsDiff = SnapshotDiffItem.DiffNode(left.permissions, right.permissions),
      metadataDiff = SnapshotDiffItem.DiffNode(left.metadata, right.metadata),
      packageSizeDiff = SnapshotDiffItem.DiffNode(left.packageSize, right.packageSize),
      isTrackItem = false
    )

    val intent = Intent(this, SnapshotDetailActivity::class.java)
      .putExtras(
        bundleOf(
          EXTRA_ENTITY to snapshotDiff,
          EXTRA_ICON to getIconsCombo(leftIconOriginal!!, rightIconOriginal!!)
        )
      )
    startActivity(intent)
  }

  private fun getSnapshotItemByUri(uri: Uri, fileName: String): SnapshotItem {
    var pi: PackageInfo? = null
    File(externalCacheDir, fileName).also { tf ->
      contentResolver.openInputStream(uri)?.use { inputStream ->
        val fileSize = inputStream.available()
        val freeSize = Environment.getExternalStorageDirectory().freeSpace

        if (freeSize > fileSize * 1.5) {
          tf.sink().buffer().use { sink ->
            inputStream.source().buffer().use {
              sink.writeAll(it)
            }
          }

          val flag = (
            PackageManager.GET_SERVICES
              or PackageManager.GET_ACTIVITIES
              or PackageManager.GET_RECEIVERS
              or PackageManager.GET_PROVIDERS
              or PackageManager.GET_PERMISSIONS
              or PackageManager.GET_META_DATA
              or PackageManager.MATCH_DISABLED_COMPONENTS
              or PackageManager.MATCH_UNINSTALLED_PACKAGES
            )
          pi = PackageManagerCompat.getPackageArchiveInfo(tf.path, flag)?.also {
            it.applicationInfo?.let { ai ->
              ai.sourceDir = tf.path
              ai.publicSourceDir = tf.path

              val iconSize = resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size)
              val appIconLoader = AppIconLoader(iconSize, false, this)
              if (fileName == Constants.TEMP_PACKAGE) {
                leftIconOriginal = appIconLoader.loadIcon(ai)
              } else {
                rightIconOriginal = appIconLoader.loadIcon(ai)
              }
            }
          }
        } else {
          showToast(R.string.toast_not_enough_storage_space)
          throw IllegalStateException("Not enough storage space")
        }
      }
    }

    pi?.let {
      val ai = it.applicationInfo ?: throw IllegalStateException("ApplicationInfo is null")
      return SnapshotItem(
        id = null,
        packageName = it.packageName,
        timeStamp = -1L,
        label = it.getAppName().toString(),
        versionName = it.versionName.toString(),
        versionCode = it.getVersionCode(),
        installedTime = it.firstInstallTime,
        lastUpdatedTime = it.lastUpdateTime,
        isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) > 0,
        abi = PackageUtils.getAbi(it).toShort(),
        targetApi = ai.targetSdkVersion.toShort(),
        nativeLibs = PackageUtils.getNativeDirLibs(it).toJson().orEmpty(),
        services = PackageUtils.getComponentStringList(it, SERVICE, false)
          .toJson().orEmpty(),
        activities = PackageUtils.getComponentStringList(it, ACTIVITY, false)
          .toJson().orEmpty(),
        receivers = PackageUtils.getComponentStringList(it, RECEIVER, false)
          .toJson().orEmpty(),
        providers = PackageUtils.getComponentStringList(it, PROVIDER, false)
          .toJson().orEmpty(),
        permissions = it.getPermissionsList().toJson().orEmpty(),
        metadata = PackageUtils.getMetaDataItems(it).toJson().orEmpty(),
        packageSize = it.getPackageSize(true),
        compileSdk = it.getCompileSdkVersion().toShort(),
        minSdk = ai.minSdkVersion.toShort()
      )
    } ?: throw IllegalStateException("PackageInfo is null")
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
