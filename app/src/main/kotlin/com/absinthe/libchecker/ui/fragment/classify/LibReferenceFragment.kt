package com.absinthe.libchecker.ui.fragment.classify

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.FragmentLibReferenceBinding
import com.absinthe.libchecker.recyclerview.LibReferenceAdapter
import com.absinthe.libchecker.recyclerview.RefListDiffUtil
import com.absinthe.libchecker.ui.main.EXTRA_NAME
import com.absinthe.libchecker.ui.main.EXTRA_TYPE
import com.absinthe.libchecker.ui.main.LibReferenceActivity
import com.absinthe.libchecker.utils.AntiShakeUtils
import com.absinthe.libchecker.viewmodel.AppViewModel

class LibReferenceFragment : Fragment(), SearchView.OnQueryTextListener {

    private val viewModel by activityViewModels<AppViewModel>()
    private val adapter = LibReferenceAdapter()

    private lateinit var binding: FragmentLibReferenceBinding
    private var isInit = false
    private var category = LibReferenceActivity.Type.TYPE_NATIVE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLibReferenceBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        if (!isInit) {
            viewModel.libReference.observe(viewLifecycleOwner, Observer {
                adapter.setNewInstance(it.toMutableList())
                binding.vfContainer.displayedChild = 1
            })
            GlobalValues.isShowSystemApps.observe(viewLifecycleOwner, Observer {
                computeRef()
            })
            GlobalValues.libReferenceThreshold.observe(viewLifecycleOwner, Observer {
                viewModel.refreshRef()
            })

            computeRef()
            isInit = true
        }
    }

    override fun onResume() {
        super.onResume()
        setHasOptionsMenu(true)
    }

    override fun onPause() {
        super.onPause()
        setHasOptionsMenu(false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.lib_ref_menu, menu)

        val searchView = SearchView(requireContext()).apply {
            setIconifiedByDefault(false)
            setOnQueryTextListener(this@LibReferenceFragment)
            queryHint = getText(R.string.search_hint)
            isQueryRefinementEnabled = true

            findViewById<View>(androidx.appcompat.R.id.search_plate).apply {
                setBackgroundColor(Color.TRANSPARENT)
            }
        }

        menu.findItem(R.id.search).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
            actionView = searchView
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.ref_category_all -> category = LibReferenceActivity.Type.TYPE_ALL
            R.id.ref_category_native -> category = LibReferenceActivity.Type.TYPE_NATIVE
            R.id.ref_category_service -> category = LibReferenceActivity.Type.TYPE_SERVICE
            R.id.ref_category_activity -> category = LibReferenceActivity.Type.TYPE_ACTIVITY
            R.id.ref_category_br -> category = LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER
            R.id.ref_category_cp -> category = LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER
        }

        if (item.itemId != R.id.search) {
            computeRef()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun computeRef() {
        binding.vfContainer.displayedChild = 0
        viewModel.computeLibReference(requireContext(), category)
    }

    private fun initView() {
        binding.rvList.adapter = this.adapter
        binding.vfContainer.apply {
            setInAnimation(activity, R.anim.anim_fade_in)
            setOutAnimation(activity, R.anim.anim_fade_out)
        }

        adapter.setDiffCallback(RefListDiffUtil())
        adapter.setOnItemClickListener { _, view, position ->
            if (AntiShakeUtils.isInvalidClick(view)) {
                return@setOnItemClickListener
            }

            val intent = Intent(requireContext(), LibReferenceActivity::class.java).apply {
                val item = this@LibReferenceFragment.adapter.data[position]
                putExtra(EXTRA_NAME, item.libName)
                putExtra(EXTRA_TYPE, item.type)
            }
            startActivity(intent)
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        viewModel.libReference.value?.let { list ->
            val filter = list.filter {
                it.libName.contains(newText) || it.chip?.name?.contains(newText) ?: false
            }
            adapter.setDiffNewData(filter.toMutableList())
        }
        return false
    }
}