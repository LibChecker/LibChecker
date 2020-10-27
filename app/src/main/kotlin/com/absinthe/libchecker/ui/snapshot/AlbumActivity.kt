package com.absinthe.libchecker.ui.snapshot

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.ActivityAlbumBinding
import com.absinthe.libchecker.extensions.dp
import com.absinthe.libchecker.ui.album.BackupActivity
import com.absinthe.libchecker.ui.album.ComparisonActivity
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumActivity : BaseActivity() {

    private lateinit var binding: ActivityAlbumBinding
    private val viewModel by viewModels<SnapshotViewModel>()
    private val itemClickObserver = MutableLiveData<Boolean>()
    private var isEasterEggAdded = false
    private var longClickCount = 0

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

        binding.itemComparison.setOnClickListener {
            startActivity(Intent(this, ComparisonActivity::class.java))
        }
        binding.itemManagement.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                var timeStampList = viewModel.repository.getTimeStamps()
                val charList = mutableListOf<String>()
                timeStampList.forEach { charList.add(viewModel.getFormatDateString(it.timestamp)) }

                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(this@AlbumActivity)
                        .setTitle(R.string.dialog_title_select_to_delete)
                        .setItems(charList.toTypedArray()) { _, which ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                viewModel.repository.deleteSnapshotsAndTimeStamp(timeStampList[which].timestamp)
                                timeStampList = viewModel.repository.getTimeStamps()
                                charList.removeAt(which)
                                GlobalValues.snapshotTimestamp = if (timeStampList.isEmpty()) {
                                    0L
                                } else {
                                    timeStampList[0].timestamp
                                }
                            }
                        }
                        .show()
                }
            }
        }
        binding.itemBackupRestore.setOnClickListener {
            startActivity(Intent(this, BackupActivity::class.java))
        }
        binding.itemTrack.setOnClickListener {
            Toasty.show(this, "Todo")
        }

        binding.itemComparison.setOnTouchListener(touchListener)
        binding.itemManagement.setOnTouchListener(touchListener)
        binding.itemBackupRestore.setOnTouchListener(touchListener)
        binding.itemTrack.setOnTouchListener(touchListener)

        itemClickObserver.observe(this, {
            if (longClickCount >= 2 && !isEasterEggAdded) {
                val easterEgg = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(200.dp, 200.dp).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    setImageResource(R.drawable.ic_album_easter_egg)
                    isVisible = false
                }
                binding.llContainer.addView(easterEgg)

                val transition = Fade().apply {
                    duration = 600
                    addTarget(easterEgg)
                }
                TransitionManager.beginDelayedTransition(binding.llContainer, transition)
                easterEgg.isVisible = true
                isEasterEggAdded = true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("ClickableViewAccessibility")
    private val touchListener = View.OnTouchListener { _, event ->
            var touchFlag = false

            when(event?.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchFlag = false
                    longClickCount++
                    itemClickObserver.value = true
                }
                MotionEvent.ACTION_UP -> {
                    longClickCount--
                    itemClickObserver.value = false

                    if (longClickCount > 1) {
                        touchFlag = true
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    touchFlag = true
                }
            }
            touchFlag
        }
    }
}