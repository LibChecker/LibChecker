package com.absinthe.libchecker.ui.snapshot

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.ActivityAlbumBinding
import com.absinthe.libchecker.ui.album.BackupActivity
import com.absinthe.libchecker.ui.album.ComparisonActivity
import com.absinthe.libchecker.ui.album.TrackActivity
import com.absinthe.libchecker.ui.fragment.snapshot.TimeNodeBottomSheetDialogFragment
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.snapshot.AlbumItemView
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.libraries.utils.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.material.app.DayNightDelegate

class AlbumActivity : BaseActivity<ActivityAlbumBinding>() {

  private val viewModel: SnapshotViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initView()
    onBackPressedDispatcher.addCallback(this, true) {
      finish()
    }
  }

  private fun initView() {
    setSupportActionBar(binding.toolbar)
    (binding.root as ViewGroup).bringChildToFront(binding.appbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    binding.toolbar.title = getString(R.string.title_album)

    val isDarkMode = when (DayNightDelegate.getDefaultNightMode()) {
      DayNightDelegate.MODE_NIGHT_YES -> true
      DayNightDelegate.MODE_NIGHT_NO -> false
      DayNightDelegate.MODE_NIGHT_FOLLOW_SYSTEM, DayNightDelegate.MODE_NIGHT_UNSPECIFIED, DayNightDelegate.MODE_NIGHT_AUTO_BATTERY -> UiUtils.isDarkModeOnSystem()
      else -> false
    }
    val itemComparison = generateAlbumItemView(
      R.drawable.ic_compare,
      if (isDarkMode) R.color.material_red_900 else R.color.material_red_300,
      R.string.album_item_comparison_title,
      R.string.album_item_comparison_subtitle
    )
    val itemManagement = generateAlbumItemView(
      R.drawable.ic_manage,
      if (isDarkMode) R.color.material_blue_900 else R.color.material_blue_300,
      R.string.album_item_management_title,
      R.string.album_item_management_subtitle
    )
    val itemBackupRestore = generateAlbumItemView(
      R.drawable.ic_backup,
      if (isDarkMode) R.color.material_green_900 else R.color.material_green_300,
      R.string.album_item_backup_restore_title,
      R.string.album_item_backup_restore_subtitle
    )
    val itemTrack = generateAlbumItemView(
      R.drawable.ic_track,
      if (isDarkMode) R.color.material_orange_900 else R.color.material_orange_300,
      R.string.album_item_track_title,
      R.string.album_item_track_subtitle
    )
    binding.llContainer.addView(itemComparison)
    binding.llContainer.addView(itemManagement)
    binding.llContainer.addView(itemBackupRestore)
    binding.llContainer.addView(itemTrack)

    itemComparison.setOnClickListener {
      startActivity(Intent(this, ComparisonActivity::class.java))
    }
    itemManagement.setOnClickListener {
      if (AntiShakeUtils.isInvalidClick(it)) {
        return@setOnClickListener
      }
      lifecycleScope.launch(Dispatchers.IO) {
        val timeStampList = viewModel.repository.getTimeStamps().toMutableList()
        withContext(Dispatchers.Main) {
          val dialog = TimeNodeBottomSheetDialogFragment
            .newInstance(ArrayList(timeStampList)).apply {
              setTitle(this@AlbumActivity.getString(R.string.dialog_title_select_to_delete))
              setOnItemClickListener { position ->
                if (position >= timeStampList.size) {
                  return@setOnItemClickListener
                }
                val item = timeStampList[position]
                lifecycleScope.launch(Dispatchers.IO) {
                  val dialog: AlertDialog
                  withContext(Dispatchers.Main) {
                    dialog = LCAppUtils.createLoadingDialog(this@AlbumActivity)
                    dialog.show()
                  }
                  viewModel.repository.deleteSnapshotsAndTimeStamp(item.timestamp)
                  if (position < timeStampList.size) {
                    timeStampList.removeAt(position)
                  }
                  GlobalValues.snapshotTimestamp = if (timeStampList.isEmpty()) {
                    0L
                  } else {
                    timeStampList[0].timestamp
                  }
                  withContext(Dispatchers.Main) {
                    root.adapter.remove(item)
                    dialog.dismiss()

                    if (timeStampList.isEmpty()) {
                      dismiss()
                    }
                  }
                }
              }
            }
          dialog.show(supportFragmentManager, TimeNodeBottomSheetDialogFragment::class.java.name)
        }
      }
    }
    itemBackupRestore.setOnClickListener {
      startActivity(Intent(this, BackupActivity::class.java))
    }
    itemTrack.setOnClickListener {
      startActivity(Intent(this, TrackActivity::class.java))
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressedDispatcher.onBackPressed()
    }
    return super.onOptionsItemSelected(item)
  }

  private fun generateAlbumItemView(
    iconRes: Int,
    iconBackgroundColorRes: Int,
    titleRes: Int,
    subtitleRes: Int
  ): AlbumItemView = AlbumItemView(ContextThemeWrapper(this, R.style.AlbumMaterialCard)).apply {
    layoutParams = ViewGroup.MarginLayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      val marginHorizontal =
        context.getDimensionPixelSize(R.dimen.album_item_margin_horizontal)
      val marginVertical = context.getDimensionPixelSize(R.dimen.album_item_margin_vertical)
      it.setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical)
    }
    container.apply {
      setIcon(iconRes)
      setIconBackgroundColor(iconBackgroundColorRes)
      title.text = getString(titleRes)
      subtitle.text = getString(subtitleRes)
    }
  }
}
