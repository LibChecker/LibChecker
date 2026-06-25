package com.absinthe.libchecker.features.snapshot.ui

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.features.applist.detail.ui.view.EmptyListView
import com.absinthe.libchecker.features.snapshot.SnapshotViewModel
import com.absinthe.libchecker.features.snapshot.ui.view.TimeNodeAddApkView
import com.absinthe.libchecker.features.snapshot.ui.view.TimeNodeAutoRemoveView
import com.absinthe.libchecker.features.snapshot.ui.view.TimeNodeBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.activityViewModel

const val EXTRA_TOP_APPS = "EXTRA_TOP_APPS"

class TimeNodeBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<TimeNodeBottomSheetView>() {

  private val viewModel: SnapshotViewModel by activityViewModel()
  private var itemClickAction: ((position: Int) -> Unit)? = null
  private var addApkClickAction: ((isLeft: Boolean) -> Unit)? = null
  private var customTitle: String? = null
  private var isCompareMode: Boolean = false
  private var isLeftMode: Boolean = false

  override fun initRootView(): TimeNodeBottomSheetView = TimeNodeBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    customTitle?.let { getHeaderView().title.text = it }
    root.adapter.apply {
      setOnItemClickListener { _, _, position ->
        itemClickAction?.invoke(position)
      }
      stateView =
        EmptyListView(context).apply {
          layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
          ).also {
            it.bottomMargin = 16.dp
          }
        }
      isStateViewEnable = true
      root.adapter.removeAllHeaderView()
      if (isCompareMode) {
        root.adapter.addHeaderView(
          TimeNodeAddApkView(requireContext()).also {
            it.setOnClickListener {
              addApkClickAction?.invoke(isLeftMode)
              dismiss()
            }
          }
        )
      } else {
        root.adapter.addHeaderView(
          TimeNodeAutoRemoveView(requireContext()).also { autoRemoveView ->
            fun recordChanged(checked: Boolean) {
              Telemetry.recordEvent(
                Constants.Event.SNAPSHOT_ADVANCED_MENU_ITEM_CHANGED,
                mapOf(
                  Telemetry.Param.CONTENT to autoRemoveView.chip.text,
                  Telemetry.Param.VALUE to checked
                )
              )
            }

            autoRemoveView.chip.onCheckedChangeListener = { _, checked ->
              autoRemoveView.invalidateText()
              if (checked) {
                val ctw = context as? ContextThemeWrapper
                if (ctw == null) {
                  autoRemoveView.syncWithAutoRemoveThreshold()
                } else {
                  var confirmedThreshold: Int? = null
                  val dialog = UiUtils.createSnapshotAutoRemoveThresholdDialog(ctw) { threshold ->
                    confirmedThreshold = threshold
                    autoRemoveView.invalidateText()
                    recordChanged(true)
                  }
                  dialog.setOnDismissListener {
                    autoRemoveView.syncWithAutoRemoveThreshold()
                    confirmedThreshold?.let { threshold ->
                      lifecycleScope.launch(Dispatchers.IO) {
                        var loadingDialog: AlertDialog? = null
                        try {
                          withContext(Dispatchers.Main) {
                            loadingDialog = UiUtils.createLoadingDialog(ctw).also { it.show() }
                          }
                          val timestampList = viewModel.retainLatestSnapshotsAndGetTimeStamps(threshold)
                          bindTimeStampItems(timestampList)
                        } finally {
                          withContext(Dispatchers.Main) {
                            loadingDialog?.dismiss()
                          }
                        }
                      }
                    }
                  }
                  dialog.show()
                }
              } else {
                GlobalValues.snapshotAutoRemoveThreshold = -1
                autoRemoveView.invalidateText()
                recordChanged(false)
              }
            }
          }
        )
      }
    }

    arguments?.let {
      BundleCompat.getParcelableArrayList(it, EXTRA_TOP_APPS, TimeStampItem::class.java)
        ?.let { topApps ->
          lifecycleScope.launch {
            bindTimeStampItems(topApps)
          }
        }
    }
  }

  fun setTitle(title: String) {
    customTitle = title
  }

  fun setCompareMode(isCompareMode: Boolean) {
    this.isCompareMode = isCompareMode
  }

  fun setLeftMode(isLeftMode: Boolean) {
    this.isLeftMode = isLeftMode
  }

  fun setOnItemClickListener(action: (position: Int) -> Unit) {
    itemClickAction = action
  }

  fun setOnAddApkClickListener(action: (isLeft: Boolean) -> Unit) {
    addApkClickAction = action
  }

  private suspend fun bindTimeStampItems(items: List<TimeStampItem>) {
    val packageIconSources = viewModel.getSnapshotPackageIconSources(getTopAppPackageNames(items))
    withContext(Dispatchers.Main) {
      if (context == null) {
        return@withContext
      }
      root.adapter.setPackageIconSources(packageIconSources)
      root.adapter.setList(items)
    }
  }

  private fun getTopAppPackageNames(items: List<TimeStampItem>): List<String> {
    return items.asSequence()
      .mapNotNull(TimeStampItem::topApps)
      .flatMap { topApps ->
        runCatching {
          topApps.fromJson<List<String>>(
            List::class.java,
            String::class.java
          ).orEmpty()
            .asSequence()
        }.getOrDefault(emptySequence())
      }
      .distinct()
      .toList()
  }

  companion object {
    fun newInstance(topApps: ArrayList<TimeStampItem>): TimeNodeBottomSheetDialogFragment {
      return TimeNodeBottomSheetDialogFragment().putArguments(
        EXTRA_TOP_APPS to topApps
      )
    }
  }
}
