package com.absinthe.libchecker.ui.fragment.snapshot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.databinding.FragmentSnapshotBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SnapshotFragment : Fragment() {

    private lateinit var binding: FragmentSnapshotBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSnapshotBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        lifecycleScope.launch {
            delay(3000)
            withContext(Dispatchers.Main) {
                binding.extendedFab.shrink()
            }
        }
    }

}