package com.absinthe.libchecker.features.album.track.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.FrameLayout
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.databinding.ActivityTrackBinding
import com.absinthe.libchecker.domain.app.GetRandomAppIconUseCase
import com.absinthe.libchecker.features.album.track.TrackListUiState
import com.absinthe.libchecker.features.album.track.TrackViewModel
import com.absinthe.libchecker.features.album.track.ui.adapter.TrackAdapter
import com.absinthe.libchecker.features.album.track.ui.adapter.TrackListDiff
import com.absinthe.libchecker.features.album.track.ui.view.TrackItemView
import com.absinthe.libchecker.features.album.track.ui.view.TrackLoadingView
import com.absinthe.libchecker.features.applist.detail.ui.view.EmptyListView
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.utils.extensions.applySystemBarsPadding
import kotlinx.coroutines.launch
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import rikka.widget.borderview.BorderView

class TrackActivity :
  BaseActivity<ActivityTrackBinding>(),
  SearchView.OnQueryTextListener,
  MenuProvider {

  private val viewModel: TrackViewModel by viewModel()
  private val getRandomAppIcon: GetRandomAppIconUseCase by inject()
  private val adapter = TrackAdapter()
  private var menu: Menu? = null
  private var isEmptyStateViewReady = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initView()
    observeTrackList()
    viewModel.loadTrackList()
  }

  private fun initView() {
    addMenuProvider(this, this, Lifecycle.State.CREATED)
    setSupportActionBar(binding.toolbar)
    (binding.root as ViewGroup).bringChildToFront(binding.appbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    binding.toolbar.title = getString(R.string.album_item_track_title)

    binding.list.apply {
      adapter = this@TrackActivity.adapter
      applySystemBarsPadding(top = true, bottom = true)
      layoutManager = LinearLayoutManager(this@TrackActivity)
      borderVisibilityChangedListener =
        BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
          binding.appbar.isLifted = !top
        }
      FastScrollerBuilder(this).useMd2Style().build()
    }
    adapter.apply {
      setDiffCallback(TrackListDiff())

      fun doSaveItemState(pos: Int, state: Boolean) {
        val packageName = data[pos].packageName
        viewModel.setPackageTracked(packageName, state)
      }

      setOnItemClickListener { _, view, position ->
        (view as TrackItemView).container.switch.apply {
          isChecked = !isChecked
          doSaveItemState(position, isChecked)
        }
      }
      setOnItemChildClickListener { _, view, position ->
        if (view.id == android.R.id.toggle) {
          doSaveItemState(position, (view as Checkable).isChecked)
        }
      }
      stateView = TrackLoadingView(this@TrackActivity) { getRandomAppIcon() }
      isStateViewEnable = true
    }
  }

  private fun observeTrackList() {
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect(::renderTrackList)
      }
    }
  }

  private fun renderTrackList(state: TrackListUiState) {
    menu?.findItem(R.id.search)?.isVisible = state.isSearchVisible
    if (!state.isLoading && !isEmptyStateViewReady) {
      adapter.stateView =
        EmptyListView(this@TrackActivity).apply {
          layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
          ).also {
            it.gravity = Gravity.CENTER
          }
        }
      adapter.isStateViewEnable = true
      isEmptyStateViewReady = true
    }
    adapter.setDiffNewData(state.items.toMutableList())
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.track_menu, menu)
    this.menu = menu

    val searchView = SearchView(this).apply {
      setIconifiedByDefault(false)
      setOnQueryTextListener(this@TrackActivity)
      queryHint = getText(R.string.search_hint)
      isQueryRefinementEnabled = true

      findViewById<View>(androidx.appcompat.R.id.search_plate).apply {
        setBackgroundColor(Color.TRANSPARENT)
      }
    }

    menu.findItem(R.id.search)?.apply {
      setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
      actionView = searchView

      if (!viewModel.uiState.value.isSearchVisible) {
        isVisible = false
      }
    }
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    if (menuItem.itemId == android.R.id.home) {
      onBackPressedDispatcher.onBackPressed()
    }
    return true
  }

  override fun onQueryTextSubmit(query: String?): Boolean {
    return false
  }

  override fun onQueryTextChange(newText: String): Boolean {
    viewModel.setQuery(newText)
    return false
  }
}
