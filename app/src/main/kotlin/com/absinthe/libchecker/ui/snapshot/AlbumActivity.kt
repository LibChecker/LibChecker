package com.absinthe.libchecker.ui.snapshot

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.ActivityAlbumBinding
import com.absinthe.libchecker.extensions.getDimensionPixelSize
import com.absinthe.libchecker.ui.album.BackupActivity
import com.absinthe.libchecker.ui.album.ComparisonActivity
import com.absinthe.libchecker.ui.album.TrackActivity
import com.absinthe.libchecker.ui.fragment.snapshot.TimeNodeBottomSheetDialogFragment
import com.absinthe.libchecker.view.snapshot.AlbumItemView
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import kotlinx.coroutines.launch

class AlbumActivity : BaseActivity() {

    private lateinit var binding: ActivityAlbumBinding
    private val viewModel by viewModels<SnapshotViewModel>()

    override fun setViewBinding(): ViewGroup {
        binding = ActivityAlbumBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView() {
        setAppBar(binding.appbar, binding.toolbar)
        (binding.root as ViewGroup).bringChildToFront(binding.appbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val itemComparison = generateAlbumItemView(
            R.drawable.ic_compare,
            R.color.material_red_300,
            R.string.album_item_comparison_title,
            R.string.album_item_comparison_subtitle
        )
        val itemManagement = generateAlbumItemView(
            R.drawable.ic_manage,
            R.color.material_blue_300,
            R.string.album_item_management_title,
            R.string.album_item_management_subtitle
        )
        val itemBackupRestore = generateAlbumItemView(
            R.drawable.ic_backup,
            R.color.material_green_300,
            R.string.album_item_backup_restore_title,
            R.string.album_item_backup_restore_subtitle
        )
        val itemTrack = generateAlbumItemView(
            R.drawable.ic_track,
            R.color.material_orange_300,
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
            lifecycleScope.launch {
                val timeStampList = viewModel.repository.getTimeStamps().toMutableList()
                val dialog = TimeNodeBottomSheetDialogFragment
                    .newInstance(ArrayList(timeStampList)).apply {
                        setTitle(getString(R.string.dialog_title_select_to_delete))
                        setOnItemClickListener { position ->
                            val item = timeStampList[position]
                            lifecycleScope.launch {
                                val progressDialog = ProgressDialog(this@AlbumActivity).apply {
                                    setMessage(getString(R.string.album_dialog_delete_snapshot_message))
                                    setCancelable(false)
                                }
                                progressDialog.show()
                                viewModel.repository.deleteSnapshotsAndTimeStamp(item.timestamp)
                                timeStampList.removeAt(position)
                                GlobalValues.snapshotTimestamp = if (timeStampList.isEmpty()) {
                                    0L
                                } else {
                                    timeStampList[0].timestamp
                                }
                                root.adapter.remove(item)
                                progressDialog.dismiss()
                            }
                        }
                    }
                dialog.show(supportFragmentManager, dialog.tag)
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
            onBackPressed()
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
        background = null
        container.apply {
            setIcon(iconRes)
            setIconBackgroundColor(iconBackgroundColorRes)
            title.text = getString(titleRes)
            subtitle.text = getString(subtitleRes)
        }
    }
}
