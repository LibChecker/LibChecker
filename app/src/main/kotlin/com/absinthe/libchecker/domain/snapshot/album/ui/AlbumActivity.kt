package com.absinthe.libchecker.domain.snapshot.album.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.backup.RoomBackup
import com.absinthe.libchecker.databinding.ActivityAlbumBinding
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.domain.snapshot.album.model.AlbumItemAction
import com.absinthe.libchecker.domain.snapshot.album.model.AlbumItemDisplayData
import com.absinthe.libchecker.domain.snapshot.album.model.buildAlbumItemDescription
import com.absinthe.libchecker.domain.snapshot.album.ui.adapter.AlbumAdapter
import com.absinthe.libchecker.domain.snapshot.backup.ui.SnapshotBackupBottomSheetDialogFragment
import com.absinthe.libchecker.domain.snapshot.backup.ui.SnapshotRoomBackupOwner
import com.absinthe.libchecker.domain.snapshot.comparison.ui.ComparisonActivity
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.timenode.ui.TimeNodeBottomSheetDialogFragment
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.extensions.addBackStateHandler
import com.absinthe.libchecker.utils.extensions.applySystemBarsPadding
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.libraries.utils.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import rikka.widget.borderview.BorderView
import timber.log.Timber

class AlbumActivity :
  BaseActivity<ActivityAlbumBinding>(),
  SnapshotRoomBackupOwner {

  private val viewModel: SnapshotViewModel by viewModel()
  private val adapter = AlbumAdapter()
  override val snapshotRoomBackup = RoomBackup(this)
  private var pendingRestoreUri: Uri? = null
  private val fragmentLifecycleCallbacks =
    object : FragmentManager.FragmentLifecycleCallbacks() {
      override fun onFragmentDetached(fragmentManager: FragmentManager, fragment: Fragment) {
        if (fragment is SnapshotBackupBottomSheetDialogFragment) {
          binding.root.post(::dispatchPendingRestoreUri)
        }
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false)
    initView()
    if (
      savedInstanceState == null &&
      intent?.action == Intent.ACTION_VIEW &&
      intent?.data != null
    ) {
      showBackupBottomSheet(intent.data)
    }
    onBackPressedDispatcher.addBackStateHandler(
      lifecycleOwner = this,
      enabledState = {
        intent?.action == Intent.ACTION_VIEW && intent?.data != null
      },
      handler = {
        startActivity(
          Intent(this, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
      }
    )
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.action != Intent.ACTION_VIEW) {
      return
    }
    val restoreUri = intent.data ?: return
    pendingRestoreUri = restoreUri
    dispatchPendingRestoreUri()
  }

  override fun onResumeFragments() {
    super.onResumeFragments()
    dispatchPendingRestoreUri()
  }

  override fun onDestroy() {
    supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
    super.onDestroy()
  }

  private fun dispatchPendingRestoreUri() {
    val restoreUri = pendingRestoreUri ?: return
    if (supportFragmentManager.isStateSaved) {
      return
    }
    val tag = SnapshotBackupBottomSheetDialogFragment::class.java.name
    val currentSheet =
      supportFragmentManager.findFragmentByTag(tag) as? SnapshotBackupBottomSheetDialogFragment
    if (currentSheet?.isRemoving == true) {
      return
    }
    if (currentSheet != null) {
      currentSheet.restoreFromLaunchUri(restoreUri)
      pendingRestoreUri = null
    } else if (showBackupBottomSheet(restoreUri)) {
      pendingRestoreUri = null
    }
  }

  private fun initView() {
    setSupportActionBar(binding.toolbar)
    (binding.root as ViewGroup).bringChildToFront(binding.appbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    binding.toolbar.title = getString(R.string.title_album)

    val isDarkMode = UiUtils.isDarkMode()
    val albumItems = listOf(
      generateAlbumItemDisplayData(
        R.drawable.ic_compare,
        if (isDarkMode) R.color.material_red_900 else R.color.material_red_300,
        R.string.album_item_comparison_title,
        R.string.album_item_comparison_subtitle,
        AlbumItemAction.Comparison
      ),
      generateAlbumItemDisplayData(
        R.drawable.ic_manage,
        if (isDarkMode) R.color.material_blue_900 else R.color.material_blue_300,
        R.string.album_item_management_title,
        R.string.album_item_management_subtitle,
        AlbumItemAction.Management
      ),
      generateAlbumItemDisplayData(
        R.drawable.ic_backup,
        if (isDarkMode) R.color.material_green_900 else R.color.material_green_300,
        R.string.album_item_backup_restore_title,
        R.string.album_item_backup_restore_subtitle,
        AlbumItemAction.BackupRestore
      ),
      generateAlbumItemDisplayData(
        R.drawable.ic_track,
        if (isDarkMode) R.color.material_orange_900 else R.color.material_orange_300,
        R.string.album_item_track_title,
        R.string.album_item_track_subtitle,
        AlbumItemAction.Track
      )
    )
    binding.llContainer.apply {
      overScrollMode = RecyclerView.OVER_SCROLL_NEVER
      adapter = this@AlbumActivity.adapter
      applySystemBarsPadding(top = true, bottom = true)
      borderVisibilityChangedListener =
        BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
          binding.appbar.isLifted = !top
        }
      layoutManager = LinearLayoutManager(context)
      isVerticalScrollBarEnabled = false
      clipToPadding = false
      clipChildren = false
      setHasFixedSize(true)

      this@AlbumActivity.adapter.apply {
        setList(albumItems)
        setOnItemClickListener { _, view, position ->
          if (AntiShakeUtils.isInvalidClick(view)) {
            return@setOnItemClickListener
          }
          when (getItem(position).action) {
            AlbumItemAction.Comparison -> startActivity(Intent(this@AlbumActivity, ComparisonActivity::class.java))
            AlbumItemAction.Management -> showSnapshotManagementDialog()
            AlbumItemAction.BackupRestore -> showBackupBottomSheet()
            AlbumItemAction.Track -> startActivity(Intent(this@AlbumActivity, TrackActivity::class.java))
          }
        }
      }
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressedDispatcher.onBackPressed()
    }
    return super.onOptionsItemSelected(item)
  }

  private fun generateAlbumItemDisplayData(
    iconRes: Int,
    iconBackgroundColorRes: Int,
    titleRes: Int,
    subtitleRes: Int,
    action: AlbumItemAction
  ): AlbumItemDisplayData {
    val title = getString(titleRes)
    val subtitle = getString(subtitleRes)
    return AlbumItemDisplayData(
      iconRes = iconRes,
      iconBackgroundColorRes = iconBackgroundColorRes,
      title = title,
      subtitle = subtitle,
      contentDescription = buildAlbumItemDescription(title, subtitle),
      action = action
    )
  }

  private fun showBackupBottomSheet(restoreUri: Uri? = null): Boolean {
    val tag = SnapshotBackupBottomSheetDialogFragment::class.java.name
    if (supportFragmentManager.findFragmentByTag(tag) != null) {
      return false
    }
    return runCatching {
      SnapshotBackupBottomSheetDialogFragment
        .newInstance(restoreUri)
        .showNow(supportFragmentManager, tag)
    }.onFailure(Timber::e).isSuccess
  }

  private fun showSnapshotManagementDialog() {
    lifecycleScope.launch(Dispatchers.IO) {
      val timeStampList = viewModel.getTimeStamps().toMutableList()
      withContext(Dispatchers.Main) {
        val dialog = TimeNodeBottomSheetDialogFragment
          .newInstance(ArrayList(timeStampList)).apply {
            setTitle(this@AlbumActivity.getString(R.string.dialog_title_select_to_delete))
            setOnItemClickListener { position ->
              if (position >= timeStampList.size) {
                return@setOnItemClickListener
              }
              val item = timeStampList[position]
              BaseAlertDialogBuilder(this@AlbumActivity)
                .setTitle(R.string.dialog_title_confirm_to_delete)
                .setMessage(viewModel.getFormatDateString(item.timestamp))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                  lifecycleScope.launch(Dispatchers.IO) {
                    val dialog: AlertDialog
                    withContext(Dispatchers.Main) {
                      dialog = com.absinthe.libchecker.utils.UiUtils.createLoadingDialog(this@AlbumActivity)
                      dialog.show()
                    }
                    val remainingTimeStamps = viewModel.deleteSnapshotTimeStamp(item.timestamp)
                    timeStampList.clear()
                    timeStampList.addAll(remainingTimeStamps)
                    withContext(Dispatchers.Main) {
                      removeItem(position)
                      dialog.dismiss()

                      if (timeStampList.isEmpty()) {
                        dismiss()
                      }
                    }
                  }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            }
          }
        dialog.show(supportFragmentManager, TimeNodeBottomSheetDialogFragment::class.java.name)
      }
    }
  }
}
