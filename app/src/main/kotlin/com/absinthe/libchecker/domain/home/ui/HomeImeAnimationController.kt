package com.absinthe.libchecker.domain.home.ui

import android.content.res.Configuration
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.view.app.FloatingBottomNavigationView
import java.util.WeakHashMap
import kotlin.math.roundToInt

/** One IME timeline for home lists, treemap and floating navigation. */
internal class HomeImeAnimationController(
  private val root: ViewGroup,
  private val navigation: View,
  private val floatingEnabled: () -> Boolean,
  private val revealNavigation: () -> Unit,
  private val invalidateBackdrop: () -> Unit
) {
  private data class Observer(val frame: (WindowInsetsCompat) -> Unit, val end: () -> Unit)
  private val observers = mutableListOf<Observer>()
  private data class ListRegistration(val padding: Int, val owner: LifecycleOwner)
  private val lists = WeakHashMap<View, ListRegistration>()
  private val bounds = Rect()
  private val density = root.resources.displayMetrics.density
  private val normalIconSize = (navigation as? FloatingBottomNavigationView)?.itemIconSize ?: 0
  private var runningAnimation: WindowInsetsAnimationCompat? = null
  private var finalInsets: WindowInsetsCompat? = null
  private var imeExtent = 0
  private var wasMini = false
  var currentInsets: WindowInsetsCompat? = null
    private set

  val miniEnabled: Boolean
    get() = OsUtils.atLeastR() && floatingEnabled() && navigation is FloatingBottomNavigationView &&
      root.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
  val miniActive: Boolean
    get() = miniEnabled && (runningAnimation != null || (currentInsets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0) > 0)
  val navigationOffset: Float
    get() = if (miniActive) navigation.translationY else 0f

  private val callback = object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
      if (animation.typeMask and WindowInsetsCompat.Type.ime() == 0) return
      runningAnimation = animation
      if (currentInsets == null) currentInsets = ViewCompat.getRootWindowInsets(root)
    }

    override fun onStart(animation: WindowInsetsAnimationCompat, bounds: WindowInsetsAnimationCompat.BoundsCompat): WindowInsetsAnimationCompat.BoundsCompat {
      if (animation === runningAnimation) imeExtent = bounds.upperBound.bottom.coerceAtLeast(1)
      return bounds
    }

    override fun onProgress(insets: WindowInsetsCompat, runningAnimations: MutableList<WindowInsetsAnimationCompat>): WindowInsetsCompat {
      if (runningAnimations.any { it.typeMask and WindowInsetsCompat.Type.ime() != 0 }) dispatch(insets)
      return insets
    }

    override fun onEnd(animation: WindowInsetsAnimationCompat) {
      if (animation !== runningAnimation) return
      runningAnimation = null
      (finalInsets ?: ViewCompat.getRootWindowInsets(root))?.let(::dispatch)
      observers.toList().forEach { it.end() }
    }
  }

  init {
    ViewCompat.setWindowInsetsAnimationCallback(root, callback)
    ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
      finalInsets = insets
      if (runningAnimation == null) {
        dispatch(insets)
      }
      insets
    }
    ViewCompat.requestApplyInsets(root)
  }

  fun registerList(view: View, owner: LifecycleOwner) {
    if (lists.containsKey(view)) return
    val footer = if (view.getTag(R.id.adapter_bottom_padding_id) == true) (96 * density).roundToInt() else 0
    lists[view] = ListRegistration((view.paddingBottom - footer).coerceAtLeast(0), owner)
    owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
      override fun onResume(owner: LifecycleOwner) {
        currentInsets?.let { updateList(view, it) }
      }
      override fun onDestroy(owner: LifecycleOwner) {
        lists.remove(view)
        owner.lifecycle.removeObserver(this)
      }
    })
    ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
      (currentInsets ?: insets).let { updateList(view, it) }
      insets
    }
    view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
      currentInsets?.let { updateList(view, it) }
    }
    currentInsets?.let { updateList(view, it) }
    ViewCompat.requestApplyInsets(view)
  }

  fun observe(owner: LifecycleOwner, frame: (WindowInsetsCompat) -> Unit, end: () -> Unit) {
    val observer = Observer(frame, end)
    observers.add(observer)
    owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
      override fun onDestroy(owner: LifecycleOwner) {
        observers.remove(observer)
        owner.lifecycle.removeObserver(this)
      }
    })
    currentInsets?.let(frame)
  }

  fun refresh() {
    currentInsets?.let(::dispatch)
  }

  private fun dispatch(insets: WindowInsetsCompat) {
    currentInsets = insets
    updateNavigation(insets)
    lists.keys.toList().forEach { updateList(it, insets) }
    observers.toList().forEach { it.frame(insets) }
    invalidateBackdrop()
  }

  private fun updateNavigation(insets: WindowInsetsCompat) {
    val nav = navigation as? FloatingBottomNavigationView ?: return
    val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
    val progress = if (!miniEnabled || imeBottom == 0) {
      0f
    } else if (runningAnimation == null) {
      1f
    } else {
      (imeBottom.toFloat() / imeExtent.coerceAtLeast(1)).coerceIn(0f, 1f)
    }
    if (progress == 0f && !wasMini) return
    if (progress > 0f && !wasMini) {
      revealNavigation()
      nav.animate().cancel()
    }
    wasMini = progress > 0f
    val systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
    nav.translationY = if (progress > 0f) -(imeBottom - systemBottom).coerceAtLeast(0).toFloat() else 0f
    nav.miniProgress = progress
    val iconSize = (normalIconSize + (20 * density - normalIconSize) * progress).roundToInt()
    if (nav.itemIconSize != iconSize) nav.itemIconSize = iconSize
    val parentWidth = (nav.parent as? View)?.width ?: root.width
    val normalMargin = root.resources.getDimensionPixelSize(R.dimen.floating_nav_bar_margin_horizontal) * nav.currentFloatingProgress
    val miniMargin = maxOf(normalMargin, (parentWidth - 224 * density) / 2)
    val margin = (normalMargin + (miniMargin - normalMargin) * progress).roundToInt()
    val params = nav.layoutParams as ViewGroup.MarginLayoutParams
    if (params.leftMargin != margin || params.rightMargin != margin) {
      params.leftMargin = margin
      params.rightMargin = margin
      nav.layoutParams = params
    }
  }

  private fun updateList(view: View, insets: WindowInsetsCompat) {
    val registration = lists[view] ?: return
    if (!registration.owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || !view.isShown || !view.isAttachedToWindow || view.height == 0) return
    bounds.setEmpty()
    root.offsetDescendantRectToMyCoords(view, bounds)
    val bottomInRoot = bounds.top + view.height
    val systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
    val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
    val footer = if (view.getTag(R.id.adapter_bottom_padding_id) == true) (96 * density).roundToInt() else 0
    var obstruction = (bottomInRoot - root.height + maxOf(systemBottom, imeBottom)).coerceAtLeast(0)
    if (miniActive) {
      bounds.setEmpty()
      root.offsetDescendantRectToMyCoords(navigation, bounds)
      obstruction = maxOf(obstruction, (bottomInRoot - bounds.top - navigation.translationY + 16 * density).roundToInt())
    }
    val bottom = registration.padding + maxOf(systemBottom + footer, obstruction)
    if (view.paddingBottom != bottom) view.updatePadding(bottom = bottom)
  }

  fun hideKeyboard(window: android.view.Window) {
    root.findFocus()?.clearFocus()
    WindowCompat.getInsetsController(window, root).hide(WindowInsetsCompat.Type.ime())
  }

  fun dispose() {
    ViewCompat.setWindowInsetsAnimationCallback(root, null)
    ViewCompat.setOnApplyWindowInsetsListener(root, null)
    observers.clear()
    lists.clear()
  }
}
