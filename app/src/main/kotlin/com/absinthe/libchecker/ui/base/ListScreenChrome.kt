package com.absinthe.libchecker.ui.base

import android.content.Context
import android.graphics.Color
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.domain.home.presentation.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal object ListScreenChrome {

  fun installSearchMenuItem(
    menuItem: MenuItem,
    context: Context,
    queryHint: CharSequence,
    retainedQuery: String,
    toolbarState: HomeViewModel.ToolbarSearchMenuState,
    listener: SearchView.OnQueryTextListener,
    isListReady: Boolean,
    onExpanded: (SearchView) -> Unit = {}
  ) {
    val initialSearchState = initialListSearchState(retainedQuery, toolbarState)
    val searchView =
      SearchView(context).apply {
        setIconifiedByDefault(false)
        this.queryHint = queryHint
        isQueryRefinementEnabled = true
        setQuery(initialSearchState.query, false)
        setOnQueryTextListener(listener)
        findViewById<View>(androidx.appcompat.R.id.search_plate).setBackgroundColor(Color.TRANSPARENT)
      }

    menuItem.apply {
      setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
      actionView = searchView
      if (initialSearchState.shouldExpand) {
        expandActionView()
        onExpanded(searchView)
      }
      isVisible = isListReady
    }
    searchView.setQuery(initialSearchState.query, false)
    searchView.setOnQueryTextListener(listener)
  }

  fun shouldRevealNavigationAfterScrollbarScroll(
    hasSeenScrollbarScroll: Boolean,
    isSearchTextClearOnce: Boolean,
    dx: Int,
    dy: Int,
    lastVisibleItemPosition: Int,
    itemCount: Int,
    isFragmentVisible: Boolean
  ): Boolean = hasSeenScrollbarScroll &&
    dx == 0 &&
    dy == 0 &&
    isFragmentVisible &&
    !isSearchTextClearOnce &&
    lastVisibleItemPosition < itemCount - 1

  fun installScrollbarNavigationReveal(
    recyclerView: RecyclerView,
    coroutineScope: CoroutineScope,
    isFragmentVisible: () -> Boolean,
    isSearchTextClearOnce: () -> Boolean,
    clearSearchTextFlag: () -> Unit,
    revealNavigation: () -> Unit,
    onScrollbarScrolled: () -> Unit = {}
  ): () -> Unit {
    var hasSeenScrollbarScroll = false
    var delayShowNavigationJob: Job? = null
    recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
      override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (dx != 0 || dy != 0) {
          clearSearchTextFlag()
          return
        }
        if (!isSearchTextClearOnce()) {
          onScrollbarScrolled()
        }
        if (!hasSeenScrollbarScroll) {
          hasSeenScrollbarScroll = true
          return
        }
        delayShowNavigationJob?.cancel()
        delayShowNavigationJob = null
        if (
          shouldRevealNavigationAfterScrollbarScroll(
            hasSeenScrollbarScroll,
            isSearchTextClearOnce(),
            dx,
            dy,
            recyclerView.layoutManager.lastVisibleItemPosition(),
            recyclerView.adapter?.itemCount ?: 0,
            isFragmentVisible()
          )
        ) {
          delayShowNavigationJob = coroutineScope.launch(Dispatchers.IO) {
            delay(SCROLLBAR_NAVIGATION_REVEAL_DELAY_MILLIS)
            withContext(Dispatchers.Main) { revealNavigation() }
          }
        }
        clearSearchTextFlag()
      }
    })
    return { hasSeenScrollbarScroll = false }
  }

  private fun RecyclerView.LayoutManager?.lastVisibleItemPosition(): Int {
    return when (this) {
      is LinearLayoutManager -> findLastVisibleItemPosition()
      is StaggeredGridLayoutManager -> findLastVisibleItemPositions(IntArray(4))[0]
      else -> 0
    }
  }

  private const val SCROLLBAR_NAVIGATION_REVEAL_DELAY_MILLIS = 400L
}
