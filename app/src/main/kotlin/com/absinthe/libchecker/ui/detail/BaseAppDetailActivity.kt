package com.absinthe.libchecker.ui.detail

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.app.CheckPackageOnResumingActivity
import com.absinthe.libchecker.ui.fragment.detail.DetailFragmentManager
import com.absinthe.libchecker.viewmodel.DetailViewModel

abstract class BaseAppDetailActivity<VB : ViewBinding> : CheckPackageOnResumingActivity<VB>(),
  IDetailContainer, SearchView.OnQueryTextListener {

  protected val viewModel: DetailViewModel by viewModels()
  protected var isListReady = false
  protected var menu: Menu? = null

  override var detailFragmentManager: DetailFragmentManager = DetailFragmentManager()

  abstract fun getToolbar(): Toolbar

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setSupportActionBar(getToolbar())
    supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setDisplayShowHomeEnabled(true)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.app_detail_menu, menu)
    this.menu = menu

    val searchView = SearchView(this).apply {
      setIconifiedByDefault(false)
      setOnQueryTextListener(this@BaseAppDetailActivity)
      queryHint = getText(R.string.search_hint)
      isQueryRefinementEnabled = true

      findViewById<View>(androidx.appcompat.R.id.search_plate).apply {
        setBackgroundColor(Color.TRANSPARENT)
      }
    }

    menu.findItem(R.id.search).apply {
      setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
      actionView = searchView

      // if (!isListReady) {
      //   isVisible = false
      // }
    }
    return super.onCreateOptionsMenu(menu)
  }

  override fun onQueryTextSubmit(query: String?): Boolean {
    return false
  }

  override fun onQueryTextChange(newText: String): Boolean {
    viewModel.queriedText = newText
    detailFragmentManager.deliverFilter(newText)
    return false
  }
}
