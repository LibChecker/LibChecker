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
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.data.app.LocalAppDataSource
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.TrackItem
import com.absinthe.libchecker.databinding.ActivityTrackBinding
import com.absinthe.libchecker.features.album.track.bean.TrackListItem
import com.absinthe.libchecker.features.album.track.ui.adapter.TrackAdapter
import com.absinthe.libchecker.features.album.track.ui.adapter.TrackListDiff
import com.absinthe.libchecker.features.album.track.ui.view.TrackItemView
import com.absinthe.libchecker.features.album.track.ui.view.TrackLoadingView
import com.absinthe.libchecker.features.applist.detail.ui.view.EmptyListView
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.utils.extensions.getAppName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import rikka.widget.borderview.BorderView

class TrackActivity :
  BaseActivity<ActivityTrackBinding>(),
  SearchView.OnQueryTextListener,
  MenuProvider {

  private val repository = Repositories.lcRepository
  private val adapter = TrackAdapter()
  private val list = mutableListOf<TrackListItem>()
  private var menu: Menu? = null
  private var isListReady = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initView()
  }

  private fun initView() {
    addMenuProvider(this, this, Lifecycle.State.STARTED)
    setSupportActionBar(binding.toolbar)
    (binding.root as ViewGroup).bringChildToFront(binding.appbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    binding.toolbar.title = getString(R.string.album_item_track_title)

    binding.list.apply {
      adapter = this@TrackActivity.adapter
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
        lifecycleScope.launch {
          val item = TrackItem(data[pos].packageName)
          if (state) {
            repository.insert(item)
          } else {
            repository.delete(item)
            repository.deleteSnapshotDiff(item.packageName)
          }
          list.find { it.packageName == data[pos].packageName }?.switchState = state
        }
        GlobalValues.trackItemsChanged = true
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
      setEmptyView(TrackLoadingView(this@TrackActivity))
    }

    lifecycleScope.launch(Dispatchers.IO) {
      val trackedList = repository.getTrackItems()
      list += LocalAppDataSource.getApplicationList()
        .asSequence()
        .map {
          TrackListItem(
            label = it.getAppName().toString(),
            packageName = it.packageName,
            switchState = trackedList.any { trackItem -> trackItem.packageName == it.packageName }
          )
        }
        .sortedByDescending { it.switchState }
        .toList()

      withContext(Dispatchers.Main) {
        adapter.setList(list)
        menu?.findItem(R.id.search)?.isVisible = true
        isListReady = true
        adapter.setEmptyView(
          EmptyListView(this@TrackActivity).apply {
            layoutParams = FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.MATCH_PARENT,
              FrameLayout.LayoutParams.MATCH_PARENT
            ).also {
              it.gravity = Gravity.CENTER
            }
          }
        )
      }
    }
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

      if (!isListReady) {
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
    adapter.setDiffNewData(
      list.asSequence()
        .filter { it.label.contains(newText, true) || it.packageName.contains(newText) }
        .sortedByDescending { it.switchState }
        .toMutableList()
    )
    return false
  }
}
