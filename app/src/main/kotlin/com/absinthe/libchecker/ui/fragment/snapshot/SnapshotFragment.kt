package com.absinthe.libchecker.ui.fragment.snapshot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.absinthe.libchecker.databinding.FragmentSnapshotBinding

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

    private fun initView() {}

}