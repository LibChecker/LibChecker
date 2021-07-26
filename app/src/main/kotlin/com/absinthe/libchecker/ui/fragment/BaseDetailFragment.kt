package com.absinthe.libchecker.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.base.BaseFragment
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.recyclerview.adapter.detail.LibStringAdapter
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.detail.IDetailContainer
import com.absinthe.libchecker.ui.fragment.detail.DetailFragmentManager
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_LIB
import com.absinthe.libchecker.ui.fragment.detail.Sortable
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.detail.EmptyListView
import com.absinthe.libchecker.viewmodel.DetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.insets.setInitialPadding
import timber.log.Timber

/**
 * <pre>
 * author : Absinthe
 * time : 2020/11/27
 * </pre>
 */

const val EXTRA_TYPE = "EXTRA_TYPE"

abstract class BaseDetailFragment<T : ViewBinding> : BaseFragment<T>(), Sortable {

  protected val viewModel: DetailViewModel by activityViewModels()
  protected val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME).orEmpty() }
  protected val type by lazy { arguments?.getInt(EXTRA_TYPE) ?: NATIVE }
  protected val adapter by lazy { LibStringAdapter(type) }
  protected val emptyView by unsafeLazy {
    EmptyListView(requireContext()).apply {
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
      ).also {
        it.gravity = Gravity.CENTER_HORIZONTAL
      }
      addPaddingTop(96.dp)
      text.text = getString(R.string.loading)
    }
  }
  protected var isListReady = false
  protected var navigateToComponentTask: Runnable? = null

  abstract fun getRecyclerView(): RecyclerView

  override fun onAttach(context: Context) {
    super.onAttach(context)
    if (context is IDetailContainer) {
      context.detailFragmentManager.register(type, this)
    }
    if (DetailFragmentManager.navType == type) {
      DetailFragmentManager.navComponent?.let {
        navigateToComponentTask = Runnable { navigateToComponentImpl(it) }
      }
      DetailFragmentManager.resetNavigationParams()
    }
  }

  override fun onStart() {
    super.onStart()
    if (!LCAppUtils.atLeastR()) {
      getRecyclerView().also {
        it.post {
          it.setInitialPadding(
            0,
            0,
            0,
            requireActivity().window.decorView.rootWindowInsets?.systemWindowInsetBottom
              ?: 0
          )
        }
      }
    }
  }

  override fun onDetach() {
    super.onDetach()
    if (requireContext() is IDetailContainer) {
      (requireContext() as IDetailContainer).detailFragmentManager.unregister(type)
    }
  }

  override suspend fun sort() {
    val list = mutableListOf<LibStringItemChip>().also {
      it += adapter.data
    }
    if (viewModel.sortMode == MODE_SORT_BY_LIB) {
      if (type == NATIVE) {
        list.sortByDescending { it.item.size }
      } else {
        list.sortByDescending { it.item.name }
      }
    } else {
      list.sortWith(compareByDescending<LibStringItemChip> { it.chip != null }.thenBy { it.item.name })
    }
    withContext(Dispatchers.Main) {
      adapter.setDiffNewData(list)
    }
  }

  fun getItemsCount() = adapter.itemCount

  @SuppressLint("NotifyDataSetChanged")
  private fun navigateToComponentImpl(component: String) {
    val componentPosition = adapter.data.indexOfFirst { it.item.name == component }
    if (componentPosition == -1) {
      return
    }

    Timber.d("navigateToComponent: componentPosition = $componentPosition")
    getRecyclerView().scrollToPosition(componentPosition.coerceAtMost(adapter.itemCount - 1))

    with(getRecyclerView().layoutManager) {
      if (this is LinearLayoutManager) {
        scrollToPositionWithOffset(componentPosition, 0)
      } else if (this is StaggeredGridLayoutManager) {
        scrollToPositionWithOffset(componentPosition, 0)
      }
    }

    adapter.setHighlightBackgroundItem(componentPosition)
    adapter.notifyDataSetChanged()
  }
}
