package com.absinthe.libchecker.ui.album

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.TrackListItem
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.database.entity.TrackItem
import com.absinthe.libchecker.databinding.ActivityTrackBinding
import com.absinthe.libchecker.recyclerview.adapter.TrackAdapter
import com.absinthe.libchecker.recyclerview.diff.TrackListDiff
import com.absinthe.libchecker.view.detail.EmptyListView
import com.absinthe.libchecker.view.snapshot.TrackItemView
import com.absinthe.libchecker.view.snapshot.TrackLoadingView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import rikka.widget.borderview.BorderView

class TrackActivity : BaseActivity(), SearchView.OnQueryTextListener {

    private lateinit var binding: ActivityTrackBinding
    private val repository = LibCheckerApp.repository
    private val adapter by lazy { TrackAdapter(lifecycleScope) }
    private val list = mutableListOf<TrackListItem>()
    private var menu: Menu? = null
    private var isListReady = false

    override fun setViewBinding(): ViewGroup {
        binding = ActivityTrackBinding.inflate(layoutInflater)
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
        binding.list.apply {
            adapter = this@TrackActivity.adapter
            layoutManager = LinearLayoutManager(this@TrackActivity)
            borderVisibilityChangedListener =
                BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
                    appBar?.setRaised(!top)
                }
            FastScrollerBuilder(this).useMd2Style().build()
        }
        adapter.apply {
            setDiffCallback(TrackListDiff())

            fun doSaveItemState(pos: Int, state: Boolean) {
                GlobalScope.launch(Dispatchers.IO) {
                    if (state) {
                        repository.insert(TrackItem(data[pos].packageName))
                    } else {
                        repository.delete(TrackItem(data[pos].packageName))
                    }
                    withContext(Dispatchers.Main) {
                        list.find { it.packageName == data[pos].packageName }?.switchState = state
                    }
                }
                AppItemRepository.trackItemsChanged = true
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

        AppItemRepository.allApplicationInfoItems.observe(this, { appList ->
            lifecycleScope.launch(Dispatchers.IO) {
                val trackedList = repository.getTrackItems()
                list.addAll(appList
                    .asSequence()
                    .map {
                        TrackListItem(
                            label = it.loadLabel(packageManager).toString(),
                            packageName = it.packageName,
                            switchState = trackedList.any { trackItem -> trackItem.packageName == it.packageName }
                        )
                    }
                    .sortedByDescending { it.switchState }
                    .toList()
                )

                withContext(Dispatchers.Main) {
                    adapter.setList(list)
                    menu?.findItem(R.id.search)?.isVisible = true
                    isListReady = true
                    adapter.setEmptyView(EmptyListView(this@TrackActivity))
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.release()
        AppItemRepository.allApplicationInfoItems.removeObservers(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
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

        menu?.findItem(R.id.search)?.apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
            actionView = searchView

            if (!isListReady) {
                isVisible = false
            }
        }
        return super.onCreateOptionsMenu(menu)
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