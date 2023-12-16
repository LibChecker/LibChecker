package com.absinthe.libchecker.features.shortcuts.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.databinding.FragmentShortcutsBinding
import com.absinthe.libchecker.ui.base.BaseListControllerFragment
import com.absinthe.libchecker.utils.Toasty

class ShortcutsFragment : BaseListControllerFragment<FragmentShortcutsBinding>() {
  override fun init() {
    Toasty.showShort(requireContext(), "Stub")
  }

  override fun onReturnTop() {
    Toasty.showShort(requireContext(), "Stub")
  }

  override fun getSuitableLayoutManager(): RecyclerView.LayoutManager? {
    return null
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    Toasty.showShort(requireContext(), "Stub")
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    return false
  }
}
