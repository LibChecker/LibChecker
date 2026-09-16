package com.absinthe.libchecker.domain.settings.ui

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.ui.base.IAppBarContainer
import com.absinthe.libchecker.ui.base.IListController
import com.absinthe.libchecker.ui.base.IListControllerHost
import com.absinthe.libchecker.ui.preference.applyM3eLayoutResources
import com.absinthe.libchecker.ui.preference.buildPreferenceItemRenderState
import com.absinthe.libchecker.ui.preference.findPreferencePosition
import com.absinthe.libchecker.ui.preference.model.PreferenceInlineControl
import com.absinthe.libchecker.ui.preference.view.PreferenceItemView
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView
import rikka.widget.borderview.BorderView
import rikka.widget.borderview.BorderViewDelegate

internal inline fun dispatchPreferenceClick(
  isInvalidClick: Boolean,
  action: () -> Unit
): Boolean {
  if (isInvalidClick) {
    return false
  }
  action()
  return true
}

abstract class BaseSettingsFragment :
  PreferenceFragmentCompat(),
  IListController {

  private companion object {
    const val STATE_EXPANDED_PREFERENCE_KEY = "expanded_preference_key"

    val NAVIGATION_PREFERENCE_KEYS = setOf(
      APPEARANCE_PREFERENCE_KEY,
      Constants.PREF_ABOUT,
      Constants.PREF_TRANSLATION,
      Constants.PREF_HELP,
      Constants.PREF_RATE,
      Constants.PREF_TELEGRAM
    )

    val INLINE_CHOICE_PREFERENCE_KEYS = setOf(
      Constants.PREF_DARK_MODE,
      Constants.PREF_SNAPSHOT_KEEP,
      Constants.PREF_RULES_REPO
    )

    val INLINE_PREFERENCE_KEYS =
      INLINE_CHOICE_PREFERENCE_KEYS + Constants.PREF_LIB_REF_THRESHOLD

    val DRAGGABLE_CHOICE_PREFERENCE_KEYS = setOf(
      Constants.PREF_DARK_MODE,
      Constants.PREF_SNAPSHOT_KEEP,
      Constants.PREF_RULES_REPO
    )
  }

  private lateinit var borderViewDelegate: BorderViewDelegate
  protected lateinit var prefRecyclerView: RecyclerView
  protected val settingsViewModel: SettingsViewModel by viewModel()
  protected var isGetUpdatesBadgeVisible = false
  private var expandedPreferenceKey: String? = null
  private var libReferenceThreshold = LIB_REFERENCE_THRESHOLD_MIN
  private var navigationView: View? = null
  private var navigationLayoutChangeListener: View.OnLayoutChangeListener? = null

  protected abstract val preferencesResource: Int

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    expandedPreferenceKey = savedInstanceState?.getString(STATE_EXPANDED_PREFERENCE_KEY)
    setPreferencesFromResource(preferencesResource, rootKey)
    preferenceScreen.applyM3eLayoutResources()
    libReferenceThreshold = normalizeLibReferenceThreshold(settingsViewModel.getLibReferenceThreshold())
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    view.setBackgroundColor(requireContext().getColorByAttr(android.R.attr.colorBackground))
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    expandedPreferenceKey?.let {
      outState.putString(STATE_EXPANDED_PREFERENCE_KEY, it)
    }
  }

  protected fun rebindVisiblePreference(preference: Preference) {
    rebindVisiblePreference(preference, animateExpansion = false)
  }

  protected fun rebindVisiblePreference(
    preference: Preference,
    animateExpansion: Boolean
  ) {
    if (!::prefRecyclerView.isInitialized) {
      return
    }

    prefRecyclerView.post {
      val adapter = prefRecyclerView.adapter as? PreferenceGroupAdapter ?: return@post
      val position = adapter.findPreferencePosition(preference) ?: return@post
      val itemView = prefRecyclerView.findViewHolderForAdapterPosition(position)?.itemView
        as? PreferenceItemView ?: return@post
      bindSettingsPreferenceItem(adapter, position, itemView, animateExpansion)
    }
  }

  protected fun bindInlinePreferenceClickListeners() {
    INLINE_PREFERENCE_KEYS.forEach { key ->
      findPreference<Preference>(key)?.setOnPreferenceClickListener { preference ->
        toggleInlinePreference(preference)
        true
      }
    }
  }

  private fun toggleInlinePreference(preference: Preference) {
    val previousKey = expandedPreferenceKey
    val nextKey = preference.key.takeUnless { it == previousKey }
    expandedPreferenceKey = nextKey

    previousKey
      ?.takeUnless { it == nextKey }
      ?.let { findPreference<Preference>(it) }
      ?.let { rebindVisiblePreference(it, animateExpansion = true) }
    nextKey
      ?.takeUnless { it == previousKey }
      ?.let { findPreference<Preference>(it) }
      ?.let { rebindVisiblePreference(it, animateExpansion = true) }
  }

  private fun buildInlineControl(preference: Preference): PreferenceInlineControl? {
    if (preference is ListPreference && preference.key in INLINE_CHOICE_PREFERENCE_KEYS) {
      val entries = preference.entries.map(CharSequence::toString)
      val entryValues = preference.entryValues.map(CharSequence::toString)
      if (preference.key == Constants.PREF_DARK_MODE) {
        return PreferenceInlineControl.IconSegmentedChoice(
          accessibilityLabels = entries,
          entryValues = entryValues,
          iconResIds = entryValues.map(::darkModePreferenceIconRes),
          selectedValue = preference.value,
          deferSelectionUntilAnimationEnd = true
        )
      }
      if (preference.key == Constants.PREF_SNAPSHOT_KEEP) {
        return PreferenceInlineControl.DraggableChoice(
          entries = entries,
          entryValues = entryValues,
          selectedValue = preference.value
        )
      }
      if (preference.key == Constants.PREF_RULES_REPO) {
        return PreferenceInlineControl.IconSegmentedChoice(
          accessibilityLabels = entries,
          entryValues = entryValues,
          iconResIds = entryValues.map {
            when (it) {
              Constants.REPO_GITHUB -> R.drawable.ic_github
              Constants.REPO_GITLAB -> R.drawable.ic_gitlab
              else -> R.drawable.ic_repository
            }
          },
          selectedValue = preference.value
        )
      }
      return null
    }
    if (preference.key == Constants.PREF_LIB_REF_THRESHOLD) {
      return PreferenceInlineControl.Range(
        value = libReferenceThreshold,
        valueFrom = LIB_REFERENCE_THRESHOLD_MIN,
        valueTo = LIB_REFERENCE_THRESHOLD_MAX
      )
    }
    return null
  }

  private fun selectInlineChoice(
    preferenceKey: String,
    value: String
  ) {
    val preference = findPreference<ListPreference>(preferenceKey) ?: return
    if (preference.value == value || !preference.callChangeListener(value)) {
      return
    }
    if (preferenceKey == Constants.PREF_DARK_MODE) {
      return
    }
    preference.value = value
    if (preferenceKey in DRAGGABLE_CHOICE_PREFERENCE_KEYS) {
      return
    }
    rebindVisiblePreference(preference)
  }

  private fun selectLibReferenceThreshold(value: Int) {
    val normalizedValue = normalizeLibReferenceThreshold(value)
    if (normalizedValue == libReferenceThreshold) {
      return
    }
    libReferenceThreshold = normalizedValue
    settingsViewModel.setLibReferenceThreshold(normalizedValue)
    recordPreferenceEvent(Constants.PREF_LIB_REF_THRESHOLD, normalizedValue.toLong())
    findPreference<Preference>(Constants.PREF_LIB_REF_THRESHOLD)?.let(::rebindVisiblePreference)
  }

  override fun onResume() {
    super.onResume()
    val container = (activity as? IAppBarContainer) ?: return
    (activity as? IListControllerHost)?.setListController(this)
    scheduleAppbarRaisingStatus(!getBorderViewDelegate().isShowingTopBorder)
    container.setLiftOnScrollTargetView(prefRecyclerView)
  }

  override fun onCreateRecyclerView(
    inflater: LayoutInflater,
    parent: ViewGroup,
    savedInstanceState: Bundle?
  ): RecyclerView {
    val recyclerView =
      super.onCreateRecyclerView(inflater, parent, savedInstanceState) as BorderRecyclerView
    recyclerView.id = android.R.id.list
    recyclerView.fixEdgeEffect()
    recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    recyclerView.isVerticalScrollBarEnabled = false
    (activity as? IAppBarContainer)?.prepareAppbarContentInset(recyclerView)
    recyclerView.applySettingsBottomPadding()

    val lp = recyclerView.layoutParams
    if (lp is FrameLayout.LayoutParams) {
      lp.rightMargin = recyclerView.context.resources.getDimension(R.dimen.normal_padding).toInt()
      lp.leftMargin = lp.rightMargin
    }

    borderViewDelegate = recyclerView.borderViewDelegate
    borderViewDelegate.borderVisibilityChangedListener =
      BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
        scheduleAppbarRaisingStatus(!top)
      }

    prefRecyclerView = recyclerView
    return recyclerView
  }

  override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
    return object : PreferenceGroupAdapter(preferenceScreen) {
      override fun onBindViewHolder(
        holder: PreferenceViewHolder,
        position: Int
      ) {
        super.onBindViewHolder(holder, position)
        (holder.itemView as? PreferenceItemView)?.let {
          bindSettingsPreferenceItem(this, position, it)
        }
      }
    }
  }

  protected fun prepareVisiblePreferenceRows() {
    val recyclerView = prefRecyclerView
    val adapter = recyclerView.adapter as? PreferenceGroupAdapter ?: return
    val owner = viewLifecycleOwner
    recyclerView.swapAdapter(null, false)
    owner.lifecycleScope.launch {
      try {
        while (!recyclerView.isLaidOut) awaitFrame()
        val viewportHeight = recyclerView.height - recyclerView.paddingTop - recyclerView.paddingBottom
        val widthSpec = View.MeasureSpec.makeMeasureSpec(
          recyclerView.width - recyclerView.paddingLeft - recyclerView.paddingRight,
          View.MeasureSpec.EXACTLY
        )
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val batchBudgetMillis = (500f / (recyclerView.display?.refreshRate ?: 60f)).toLong().coerceAtLeast(1)
        val counts = mutableMapOf<Int, Int>()
        val holders = mutableListOf<PreferenceViewHolder>()
        var preparedHeight = 0
        var position = 0
        // Prepare only the requested viewport, yielding between small batches.
        while (position < adapter.itemCount && preparedHeight < viewportHeight) {
          awaitFrame()
          val deadline = SystemClock.uptimeMillis() + batchBudgetMillis
          do {
            val type = adapter.getItemViewType(position)
            val holder = adapter.createViewHolder(recyclerView, type)
            adapter.bindViewHolder(holder, position++)
            holder.itemView.measure(widthSpec, heightSpec)
            val margins = holder.itemView.layoutParams as? ViewGroup.MarginLayoutParams
            preparedHeight += holder.itemView.measuredHeight +
              (margins?.topMargin ?: 0) + (margins?.bottomMargin ?: 0)
            val count = (counts[type] ?: 0) + 1
            counts[type] = count
            recyclerView.recycledViewPool.setMaxRecycledViews(type, maxOf(5, count))
            holders += holder
          } while (
            position < adapter.itemCount && preparedHeight < viewportHeight &&
            SystemClock.uptimeMillis() < deadline
          )
        }
        holders.asReversed().forEach(recyclerView.recycledViewPool::putRecycledView)
      } finally {
        if (owner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
          recyclerView.swapAdapter(adapter, false)
        }
      }
    }
  }

  private fun scheduleAppbarRaisingStatus(isLifted: Boolean) {
    val host = activity as? IListControllerHost ?: return
    if (host.isCurrentListController(this)) {
      (activity as? IAppBarContainer)?.scheduleAppbarLiftingStatus(isLifted)
    }
  }

  override fun onDetach() {
    super.onDetach()
    (activity as? IListControllerHost)?.clearListController(this)
  }

  override fun onDestroyView() {
    navigationLayoutChangeListener?.let { listener ->
      navigationView?.removeOnLayoutChangeListener(listener)
    }
    navigationLayoutChangeListener = null
    navigationView = null
    super.onDestroyView()
  }

  override fun onReturnTop() {
    // Do nothing
  }

  override fun getBorderViewDelegate(): BorderViewDelegate = borderViewDelegate
  override fun isAllowRefreshing(): Boolean = true
  override fun getSuitableLayoutManager(): RecyclerView.LayoutManager? = null

  private fun bindSettingsPreferenceItem(
    adapter: PreferenceGroupAdapter,
    position: Int,
    itemView: PreferenceItemView,
    animateExpansion: Boolean = false
  ) {
    val state = adapter.buildPreferenceItemRenderState(
      position = position,
      showChevron = { it.key in NAVIGATION_PREFERENCE_KEYS },
      badgeDescription = {
        if (it.key == Constants.PREF_GET_UPDATES && isGetUpdatesBadgeVisible) {
          getString(R.string.settings_update_available)
        } else {
          null
        }
      },
      inlineControl = ::buildInlineControl,
      expanded = { it.key == expandedPreferenceKey }
    ) ?: return
    itemView.bind(
      state = state,
      animateExpansion = animateExpansion,
      onChoiceSelected = { value ->
        state.preferenceKey?.let { selectInlineChoice(it, value) }
      },
      onRangeValueChangeFinished = ::selectLibReferenceThreshold
    )
  }

  private fun RecyclerView.applySettingsBottomPadding() {
    val basePadding = resources.getDimensionPixelSize(R.dimen.settings_list_vertical_padding)
    val appNavigationView = activity?.findViewById<View>(R.id.nav_view)
    var systemBarBottomInset = 0

    fun updateBottomPadding() {
      val bottomNavigationView = appNavigationView as? BottomNavigationView
      val bottomNavigationHeight = bottomNavigationView
        ?.height
        ?.takeIf { it > 0 }
      val bottomNavigationBottomMargin = (bottomNavigationView?.layoutParams as? ViewGroup.MarginLayoutParams)
        ?.bottomMargin ?: 0
      updatePadding(
        bottom = calculateSettingsBottomPadding(
          basePadding = basePadding,
          systemBarBottomInset = systemBarBottomInset,
          bottomNavigationHeight = bottomNavigationHeight,
          bottomNavigationBottomMargin = bottomNavigationBottomMargin
        )
      )
    }

    ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
      systemBarBottomInset =
        windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
      updateBottomPadding()
      windowInsets
    }

    val listener =
      View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateBottomPadding() }
    appNavigationView?.addOnLayoutChangeListener(listener)
    navigationView = appNavigationView
    navigationLayoutChangeListener = listener

    doOnAttach {
      ViewCompat.requestApplyInsets(it)
      updateBottomPadding()
    }
  }

  protected fun recordPreferenceEvent(key: String, value: Any = "") {
    Telemetry.recordEvent(
      Constants.Event.SETTINGS,
      mapOf(Telemetry.Param.CONTENT to key, Telemetry.Param.VALUE to value)
    )
  }
}
