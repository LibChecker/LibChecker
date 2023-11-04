package com.absinthe.libchecker.features.album.ui

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.ActivityAlbumBinding
import com.absinthe.libchecker.features.album.backup.ui.BackupActivity
import com.absinthe.libchecker.features.album.comparison.ui.ComparisonActivity
import com.absinthe.libchecker.features.album.track.ui.TrackActivity
import com.absinthe.libchecker.features.album.ui.adapter.AlbumAdapter
import com.absinthe.libchecker.features.album.ui.view.AlbumItemView
import com.absinthe.libchecker.features.snapshot.SnapshotViewModel
import com.absinthe.libchecker.features.snapshot.ui.TimeNodeBottomSheetDialogFragment
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.libraries.utils.utils.UiUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.widget.borderview.BorderView

class AlbumActivity : BaseActivity<ActivityAlbumBinding>() {

  private val viewModel: SnapshotViewModel by viewModels()
  private val adapter = AlbumAdapter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initView()
  }

  private fun initView() {
    setSupportActionBar(binding.toolbar)
    (binding.root as ViewGroup).bringChildToFront(binding.appbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    binding.toolbar.title = getString(R.string.title_album)

    val isDarkMode = UiUtils.isDarkMode()
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
    binding.llContainer.apply {
      overScrollMode = RecyclerView.OVER_SCROLL_NEVER
      adapter = this@AlbumActivity.adapter
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
        addData(itemComparison)
        addData(itemManagement)
        addData(itemBackupRestore)
        addData(itemTrack)
      }
    }

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
                BaseAlertDialogBuilder(this@AlbumActivity)
                  .setTitle(R.string.dialog_title_confirm_to_delete)
                  .setMessage(getFormatDateString(item.timestamp))
                  .setPositiveButton(android.R.string.ok) { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                      val dialog: AlertDialog
                      withContext(Dispatchers.Main) {
                        dialog = com.absinthe.libchecker.utils.UiUtils.createLoadingDialog(this@AlbumActivity)
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
                  .setNegativeButton(android.R.string.cancel, null)
                  .show()
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

  private fun getFormatDateString(timestamp: Long): String {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.getDefault())
    val date = Date(timestamp)
    return simpleDateFormat.format(date)
  }
}
