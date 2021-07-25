package com.absinthe.libchecker.ui.album

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.databinding.ActivityBackupBinding
import com.absinthe.libchecker.utils.StorageUtils
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView
import rikka.widget.borderview.BorderView
import timber.log.Timber
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupActivity : BaseActivity() {

  private lateinit var binding: ActivityBackupBinding

  override fun setViewBinding(): ViewGroup {
    binding = ActivityBackupBinding.inflate(layoutInflater)
    return binding.root
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setAppBar(binding.appbar, binding.toolbar)
    (binding.root as ViewGroup).bringChildToFront(binding.appbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, BackupFragment())
        .commit()
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressed()
    }
    return super.onOptionsItemSelected(item)
  }

  class BackupFragment : PreferenceFragmentCompat() {

    private val viewModel by viewModels<SnapshotViewModel>()

    private lateinit var backupResultLauncher: ActivityResultLauncher<String>
    private lateinit var restoreResultLauncher: ActivityResultLauncher<String>

    override fun onAttach(context: Context) {
      super.onAttach(context)
      backupResultLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) {
          it?.let {
            try {
              requireActivity().contentResolver.openOutputStream(it)?.let { os ->
                viewModel.backup(os)
              }
            } catch (e: IOException) {
              Timber.e(e)
            }
          }
        }
      restoreResultLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
          it?.let {
            try {
              requireActivity().contentResolver.openInputStream(it)
                ?.let { inputStream ->
                  viewModel.restore(inputStream) {
                    context.showToast("Backup file error")
                  }
                }
            } catch (e: IOException) {
              Timber.e(e)
            }
          }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      setPreferencesFromResource(R.xml.album_backup, rootKey)

      findPreference<Preference>(Constants.PREF_LOCAL_BACKUP)?.apply {
        setOnPreferenceClickListener {
          val simpleDateFormat =
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
          val date = Date()
          val formatted = simpleDateFormat.format(date)

          if (StorageUtils.isExternalStorageWritable) {
            backupResultLauncher.launch("LibChecker-Snapshot-Backups-$formatted.lcss")
          } else {
            context.showToast("External storage is not writable")
          }
          true
        }
      }
      findPreference<Preference>(Constants.PREF_LOCAL_RESTORE)?.apply {
        setOnPreferenceClickListener {
          restoreResultLauncher.launch("*/*")
          true
        }
      }
    }

    override fun onCreateRecyclerView(
      inflater: LayoutInflater,
      parent: ViewGroup,
      savedInstanceState: Bundle?
    ): RecyclerView {
      val recyclerView = super.onCreateRecyclerView(
        inflater,
        parent,
        savedInstanceState
      ) as BorderRecyclerView
      recyclerView.fixEdgeEffect()
      recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER

      val lp = recyclerView.layoutParams
      if (lp is FrameLayout.LayoutParams) {
        lp.rightMargin =
          recyclerView.context.resources.getDimension(R.dimen.rd_activity_horizontal_margin)
            .toInt()
        lp.leftMargin = lp.rightMargin
      }

      recyclerView.borderViewDelegate.borderVisibilityChangedListener =
        BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
          (activity as BaseActivity?)?.appBar?.setRaised(!top)
        }
      return recyclerView
    }
  }
}
