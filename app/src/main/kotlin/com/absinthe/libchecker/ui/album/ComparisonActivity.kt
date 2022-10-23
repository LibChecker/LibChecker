package com.absinthe.libchecker.ui.album

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.compat.BundleCompat
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.databinding.ActivityComparisonBinding
import com.absinthe.libchecker.recyclerview.HorizontalSpacesItemDecoration
import com.absinthe.libchecker.recyclerview.adapter.snapshot.SnapshotAdapter
import com.absinthe.libchecker.ui.detail.EXTRA_ENTITY
import com.absinthe.libchecker.ui.detail.SnapshotDetailActivity
import com.absinthe.libchecker.ui.fragment.snapshot.TimeNodeBottomSheetDialogFragment
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.PackageUtils.getPermissionsList
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libchecker.utils.toJson
import com.absinthe.libchecker.view.snapshot.ComparisonDashboardView
import com.absinthe.libchecker.view.snapshot.SnapshotEmptyView
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import rikka.widget.borderview.BorderView
import timber.log.Timber

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
      FileUtils.delete(File(Constants.TEMP_PACKAGE))
      FileUtils.delete(File(Constants.TEMP_PACKAGE_2))
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressedDispatcher.onBackPressed()
    }
    return super.onOptionsItemSelected(item)
  }

  private fun registerCallbacks() {
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          finish()
        }
      }
    )
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
          } else {
            compareDiffContainsApk()
          }

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
      snapshotDiffItems.observe(this@ComparisonActivity) { list ->
        adapter.setList(list.sortedByDescending { it.updateTime })
        flip(VF_LIST)
      }
      lifecycleScope.launchWhenStarted {
        effect.collect {
          when (it) {
            is SnapshotViewModel.Effect.ChooseComparedApk -> {
              isLeftPartChoosing = it.isLeftPart
              chooseApkResultLauncher.launch("application/vnd.android.package-archive")
            }
          }
        }
      }
    }
  }

  private fun parseIntent(intent: Intent) {
    if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
      intent.extras?.let {
        val uriList = BundleCompat.getParcelableArrayList<Uri>(it, Intent.EXTRA_STREAM)
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
          lifecycleScope.launch(Dispatchers.IO) {
            val count = viewModel.repository.getSnapshots(leftTimeStamp).size
            withContext(Dispatchers.Main) {
              it.leftPart.tvSnapshotAppsCountText.text = count.toString()
            }
          }
        }

        if (rightTimeStamp == -1L) {
          rightUri?.encodedPath?.let { path ->
            it.rightPart.tvSnapshotTimestampText.text = getUriFileName(path)
          }
          it.rightPart.tvSnapshotAppsCountText.text = 1.toString()
        } else if (rightTimeStamp > 0) {
          it.rightPart.tvSnapshotTimestampText.text = viewModel.getFormatDateString(rightTimeStamp)
          lifecycleScope.launch(Dispatchers.IO) {
            val count = viewModel.repository.getSnapshots(rightTimeStamp).size
            withContext(Dispatchers.Main) {
              it.rightPart.tvSnapshotAppsCountText.text = count.toString()
            }
          }
        }
      }
    }
  }

  private fun getUriFileName(path: String): String {
    val decode = Uri.decode(path)
    return if (decode.contains("/")) {
      getUriFileName(decode.split("/").last())
    } else {
      decode
    }
  }

  private fun compareDiffContainsApk() = lifecycleScope.launch(Dispatchers.IO) {
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

    viewModel.compareDiffWithSnapshotList(-1, leftSnapshots, rightSnapshots)
  }

  private fun getSnapshotItemByUri(uri: Uri, fileName: String): SnapshotItem {
    var pi: PackageInfo? = null
    File(externalCacheDir, fileName).also { tf ->
      contentResolver.openInputStream(uri)?.use { inputStream ->
        val fileSize = inputStream.available()
        val freeSize = Environment.getExternalStorageDirectory().freeSpace
        Timber.d("fileSize=$fileSize, freeSize=$freeSize")

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
            it.applicationInfo.sourceDir = tf.path
            it.applicationInfo.publicSourceDir = tf.path
          }
        } else {
          showToast(R.string.toast_not_enough_storage_space)
          throw IllegalStateException("Not enough storage space")
        }
      }
    }

    if (pi == null) {
      throw IllegalStateException("PackageInfo is null")
    }
    val ai = pi!!.applicationInfo
    return SnapshotItem(
      id = null,
      packageName = pi!!.packageName,
      timeStamp = -1L,
      label = ai.loadLabel(packageManager).toString(),
      versionName = pi!!.versionName ?: "null",
      versionCode = PackageUtils.getVersionCode(pi!!),
      installedTime = pi!!.firstInstallTime,
      lastUpdatedTime = pi!!.lastUpdateTime,
      isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
      abi = PackageUtils.getAbi(pi!!).toShort(),
      targetApi = ai.targetSdkVersion.toShort(),
      nativeLibs = PackageUtils.getNativeDirLibs(pi!!).toJson().orEmpty(),
      services = PackageUtils.getComponentStringList(pi!!.packageName, SERVICE, false)
        .toJson().orEmpty(),
      activities = PackageUtils.getComponentStringList(pi!!.packageName, ACTIVITY, false)
        .toJson().orEmpty(),
      receivers = PackageUtils.getComponentStringList(pi!!.packageName, RECEIVER, false)
        .toJson().orEmpty(),
      providers = PackageUtils.getComponentStringList(pi!!.packageName, PROVIDER, false)
        .toJson().orEmpty(),
      permissions = pi!!.getPermissionsList().toJson().orEmpty(),
      metadata = PackageUtils.getMetaDataItems(pi!!).toJson().orEmpty(),
      packageSize = PackageUtils.getPackageSize(pi!!, true)
    )
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
