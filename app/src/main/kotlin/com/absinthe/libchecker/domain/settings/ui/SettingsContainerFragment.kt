package com.absinthe.libchecker.domain.settings.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.home.ui.MainActivity

internal const val APPEARANCE_PREFERENCE_KEY = "appearance"

class SettingsContainerFragment : Fragment(R.layout.fragment_settings_container) {
  private val backStackListener = FragmentManager.OnBackStackChangedListener { updateNavigation() }

  val isAppearanceVisible: Boolean
    get() = childFragmentManager.findFragmentById(R.id.settings_content) is AppearanceSettingsFragment

  private val backCallback = object : OnBackPressedCallback(false) {
    override fun handleOnBackPressed() {
      closeAppearance()
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
    childFragmentManager.addOnBackStackChangedListener(backStackListener)
    if (savedInstanceState == null) {
      childFragmentManager.commitNow {
        replace(R.id.settings_content, SettingsFragment::class.java, null)
      }
    }
  }

  fun openAppearance() {
    if (childFragmentManager.isStateSaved || isAppearanceVisible) return
    childFragmentManager.commit {
      setReorderingAllowed(true)
      setTransition(FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN)
      replace(R.id.settings_content, AppearanceSettingsFragment::class.java, null)
      addToBackStack(APPEARANCE_PREFERENCE_KEY)
    }
  }

  fun closeAppearance() {
    if (!childFragmentManager.isStateSaved) {
      childFragmentManager.popBackStackImmediate()
    }
  }

  override fun onResume() {
    super.onResume()
    updateNavigation()
  }

  override fun onPause() {
    backCallback.isEnabled = false
    super.onPause()
  }

  override fun onDestroyView() {
    childFragmentManager.removeOnBackStackChangedListener(backStackListener)
    super.onDestroyView()
  }

  private fun updateNavigation() {
    backCallback.isEnabled = isResumed && isAppearanceVisible
    (activity as? MainActivity)?.updateSettingsNavigation()
  }
}
