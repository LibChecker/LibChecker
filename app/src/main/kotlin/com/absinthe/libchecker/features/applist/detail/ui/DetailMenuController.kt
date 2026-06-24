package com.absinthe.libchecker.features.applist.detail.ui

import android.content.Context
import android.graphics.Color
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
import com.absinthe.libchecker.R

class DetailMenuController(
  private val context: Context,
  private val toolbar: Toolbar,
  private val onNavigateUp: () -> Unit,
  private val onQueryTextChanged: (String) -> Unit
) : MenuProvider,
  SearchView.OnQueryTextListener {

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.app_detail_menu, menu)

    val searchView = SearchView(context).apply {
      setIconifiedByDefault(false)
      setOnQueryTextListener(this@DetailMenuController)
      queryHint = context.getText(R.string.search_hint)
      isQueryRefinementEnabled = true

      findViewById<View>(androidx.appcompat.R.id.search_plate).apply {
        setBackgroundColor(Color.TRANSPARENT)
      }
    }

    menu.findItem(R.id.search).apply {
      setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
      actionView = searchView
    }
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    if (menuItem.itemId == android.R.id.home) {
      onNavigateUp()
    }
    return true
  }

  override fun onQueryTextSubmit(query: String?): Boolean {
    return false
  }

  override fun onQueryTextChange(newText: String): Boolean {
    onQueryTextChanged(newText)
    return false
  }

  fun hasExpandedActionView(): Boolean {
    return toolbar.hasExpandedActionView()
  }

  fun collapseActionView() {
    toolbar.collapseActionView()
  }
}
