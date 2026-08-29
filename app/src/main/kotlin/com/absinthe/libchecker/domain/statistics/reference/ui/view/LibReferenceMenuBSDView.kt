package com.absinthe.libchecker.domain.statistics.reference.ui.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.doOnNextLayout
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceListRenderState
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceMenuAction
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceMenuBottomSheetState
import com.absinthe.libchecker.domain.statistics.reference.ui.adapter.LibReferenceAdapter
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.ui.app.MenuOptionItemView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.setSingleChild
import com.absinthe.libchecker.view.app.BottomSheetScaffoldView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.motion.MotionUtils

class LibReferenceMenuBSDView(
  context: Context,
  private val onDemoHeightAnimationStateChange: (Boolean) -> Unit = {}
) : BottomSheetScaffoldView(context) {

  private var onAction: (LibReferenceMenuAction) -> Unit = {}
  private var hasRenderedState = false
  private val demoTransitionGate = LibReferenceDemoTransitionGate()
  private val demoTransitionQueue = LibReferenceDemoTransitionQueue<PendingDemoBind>()
  private var activeHeightSpringGeneration = 0
  private var isDemoHeightAnimationRunning = false
  private var demoRemovalAnimator: ValueAnimator? = null
  private var demoInsertionAnimator: ValueAnimator? = null
  private var pendingDemoInsertion: PendingDemoInsertion? = null
  private var preparedDemoInsertion: PreparedDemoInsertion? = null
  private var activeDemoInsertion: ActiveDemoInsertion? = null
  private var activeDemoRemoval: ActiveDemoRemoval? = null

  private val demoMotionDuration = MotionUtils.resolveThemeDuration(
    context,
    com.google.android.material.R.attr.motionDurationMedium3,
    DEFAULT_DEMO_MOTION_DURATION
  ).toLong()

  private val demoEffectDuration = MotionUtils.resolveThemeDuration(
    context,
    com.google.android.material.R.attr.motionDurationShort3,
    DEFAULT_DEMO_EFFECT_DURATION
  ).toLong()

  private val heightSpring = SpringAnimation(this, HEIGHT_PROPERTY).apply {
    spring = MotionUtils.resolveThemeSpringForce(
      context,
      com.google.android.material.R.attr.motionSpringDefaultSpatial,
      com.google.android.material.R.style.Motion_Material3_Spring_Expressive_Default_Spatial
    )
    addEndListener { _, cancelled, _, _ ->
      if (!cancelled && demoTransitionGate.isCurrent(activeHeightSpringGeneration)) {
        restoreWrapContentHeight()
        finishDemoHeightAnimationAfterLayout(activeHeightSpringGeneration)
      }
    }
  }

  private val demoAdapter = LibReferenceAdapter(
    allowDetailAction = false,
    onAction = {},
    onItemBound = ::preparePendingDemoInsertion
  )
  private val optionsAdapter = OptionsAdapter()
  private val demoItemAnimator = DefaultItemAnimator().apply {
    addDuration = demoEffectDuration
    removeDuration = demoEffectDuration
    moveDuration = demoMotionDuration
    changeDuration = demoEffectDuration
    supportsChangeAnimations = false
  }
  private val demoDividerMargin = 8.dp

  private val optionsLayout = FlexboxLayout(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.bottomMargin = 8.dp
    }
    flexWrap = FlexWrap.WRAP
    justifyContent = JustifyContent.FLEX_START
    flexDirection = FlexDirection.ROW
  }

  private val demoDivider = MaterialDivider(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = demoDividerMargin
      it.bottomMargin = demoDividerMargin
    }
    dividerInsetStart = 4.dp
    dividerInsetEnd = 4.dp
    dividerThickness = 1.dp
    dividerColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutlineVariant)
  }

  private val optionsContainer = LinearLayout(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    orientation = VERTICAL
    addView(demoDivider)
    addView(optionsLayout)
  }

  private val list = BottomSheetRecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    overScrollMode = OVER_SCROLL_NEVER
    adapter = ConcatAdapter(demoAdapter, optionsAdapter)
    layoutManager = LinearLayoutManager(context)
    itemAnimator = demoItemAnimator
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    isNestedScrollingEnabled = true
  }

  init {
    val padding = 16.dp
    setPadding(padding, padding, padding, 0)
    header.title.text = context.getString(R.string.advanced_menu)
    addView(list)
    optionsAdapter.setList(listOf(Unit))
  }

  fun bind(
    state: LibReferenceMenuBottomSheetState,
    onAction: (LibReferenceMenuAction) -> Unit
  ) {
    this.onAction = onAction
    renderOptionButtons(state)
    val pendingBind = PendingDemoBind(state, onAction)
    demoTransitionQueue.offer(pendingBind)?.let(::render)
  }

  private fun render(pendingBind: PendingDemoBind) {
    val state = pendingBind.state
    val transitionGeneration = demoTransitionGate.advance()
    cancelOngoingDemoTransition()
    onAction = pendingBind.onAction
    val currentDemoItems = demoAdapter.data.filterIsInstance<LibReference>()
    demoAdapter.bind(
      LibReferenceListRenderState(
        colorfulRuleIcon = state.colorfulRuleIcon,
        labelSuffix = context.getString(R.string.lib_reference_demo_example_suffix)
      )
    )
    if (
      hasRenderedState &&
      isLaidOut &&
      (
        animateSingleDemoRemovalIfPossible(currentDemoItems, state, transitionGeneration) ||
          animateSingleDemoInsertionIfPossible(currentDemoItems, state, transitionGeneration)
        )
    ) {
      return
    }

    val shouldAnimateHeight = hasRenderedState && isLaidOut && demoItemsChanged(state.demoItems)
    if (shouldAnimateHeight) {
      freezeCurrentHeight()
    }
    if (hasRenderedState) {
      demoAdapter.setDiffNewData(state.demoItems) {
        if (shouldAnimateHeight) {
          scheduleMeasuredContentHeightAnimation(
            generation = transitionGeneration,
            delayMillis = 0L
          )
        }
      }
    } else {
      demoAdapter.setList(state.demoItems)
      hasRenderedState = true
    }
    renderDemoDivider(state)
    if (!shouldAnimateHeight) {
      completeDemoTransition(transitionGeneration)
    }
  }

  override fun onDetachedFromWindow() {
    demoTransitionQueue.clear()
    demoTransitionGate.advance()
    cancelOngoingDemoTransition()
    setDemoHeightAnimationRunning(false)
    onAction = {}
    super.onDetachedFromWindow()
  }

  private fun animateSingleDemoInsertionIfPossible(
    currentItems: List<LibReference>,
    nextState: LibReferenceMenuBottomSheetState,
    transitionGeneration: Int
  ): Boolean {
    val plan = planLibReferenceDemoUpdate(currentItems, nextState.demoItems)
    if (plan !is LibReferenceDemoUpdatePlan.AnimateInsertion) return false
    val insertedItem = nextState.demoItems[plan.insertedIndex]

    setDemoHeightAnimationRunning(true)
    list.itemAnimator = null
    val expandDivider = currentItems.isEmpty()
    val dividerParams = demoDivider.layoutParams as ViewGroup.MarginLayoutParams
    val dividerTargetHeight = demoDivider.dividerThickness
    val dividerTargetTopMargin = dividerParams.topMargin
    val dividerTargetBottomMargin = dividerParams.bottomMargin
    if (expandDivider) {
      dividerParams.height = 0
      dividerParams.topMargin = 0
      dividerParams.bottomMargin = 0
      demoDivider.layoutParams = dividerParams
      demoDivider.alpha = 0f
      demoDivider.visibility = View.VISIBLE
    }
    pendingDemoInsertion = PendingDemoInsertion(
      identity = insertedItem.libName to insertedItem.type,
      generation = transitionGeneration
    )
    preparedDemoInsertion = null
    renderDemoDivider(nextState)
    demoAdapter.setDiffNewData(nextState.demoItems) {
      if (!demoTransitionGate.isCurrent(transitionGeneration)) return@setDiffNewData
      list.doOnNextLayout {
        if (!demoTransitionGate.isCurrent(transitionGeneration)) return@doOnNextLayout
        val preparedInsertion = preparedDemoInsertion
        pendingDemoInsertion = null
        preparedDemoInsertion = null
        if (preparedInsertion == null) {
          restoreDemoDividerGeometry()
          list.itemAnimator = demoItemAnimator
          finishDemoHeightAnimationAfterLayout(transitionGeneration)
        } else {
          animatePreparedDemoInsertion(
            preparedInsertion = preparedInsertion,
            transitionGeneration = transitionGeneration,
            expandDivider = expandDivider,
            dividerParams = dividerParams,
            dividerTargetHeight = dividerTargetHeight,
            dividerTargetTopMargin = dividerTargetTopMargin,
            dividerTargetBottomMargin = dividerTargetBottomMargin
          )
        }
      }
    }
    return true
  }

  private fun preparePendingDemoInsertion(reference: LibReference, itemView: View) {
    val pendingInsertion = pendingDemoInsertion ?: return
    if (!demoTransitionGate.isCurrent(pendingInsertion.generation)) return
    if (pendingInsertion.identity.first != reference.libName || pendingInsertion.identity.second != reference.type) return
    if (preparedDemoInsertion?.itemView === itemView) return
    val itemParams = itemView.layoutParams
    val marginParams = itemParams as? ViewGroup.MarginLayoutParams
    val targetTopMargin = marginParams?.topMargin ?: 0
    val targetBottomMargin = marginParams?.bottomMargin ?: 0
    val targetWidth = (list.width - list.paddingStart - list.paddingEnd).coerceAtLeast(0)
    if (targetWidth == 0) return
    itemParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
    itemView.layoutParams = itemParams
    itemView.measure(
      View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )
    preparedDemoInsertion = PreparedDemoInsertion(
      itemView = itemView,
      itemParams = itemParams,
      targetHeight = itemView.measuredHeight,
      targetTopMargin = targetTopMargin,
      targetBottomMargin = targetBottomMargin
    )
    itemParams.height = 0
    marginParams?.topMargin = 0
    marginParams?.bottomMargin = 0
    itemView.layoutParams = itemParams
    itemView.alpha = 0f
  }

  private fun animatePreparedDemoInsertion(
    preparedInsertion: PreparedDemoInsertion,
    transitionGeneration: Int,
    expandDivider: Boolean,
    dividerParams: ViewGroup.MarginLayoutParams,
    dividerTargetHeight: Int,
    dividerTargetTopMargin: Int,
    dividerTargetBottomMargin: Int
  ) {
    val marginParams = preparedInsertion.itemParams as? ViewGroup.MarginLayoutParams
    val insertion = ActiveDemoInsertion(
      preparedInsertion = preparedInsertion,
      holder = list.findContainingViewHolder(preparedInsertion.itemView)
    ).also { activeInsertion ->
      activeInsertion.holder?.setIsRecyclable(false)
      activeInsertion.isRecyclabilityLocked = activeInsertion.holder != null
    }
    activeDemoInsertion = insertion
    demoInsertionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
      duration = demoMotionDuration
      interpolator = FastOutSlowInInterpolator()
      addUpdateListener { animator ->
        val progress = animator.animatedFraction
        preparedInsertion.itemParams.height = (preparedInsertion.targetHeight * progress).toInt()
        marginParams?.topMargin = (preparedInsertion.targetTopMargin * progress).toInt()
        marginParams?.bottomMargin = (preparedInsertion.targetBottomMargin * progress).toInt()
        preparedInsertion.itemView.layoutParams = preparedInsertion.itemParams
        preparedInsertion.itemView.alpha = progress
        if (expandDivider) {
          dividerParams.height = (dividerTargetHeight * progress).toInt()
          dividerParams.topMargin = (dividerTargetTopMargin * progress).toInt()
          dividerParams.bottomMargin = (dividerTargetBottomMargin * progress).toInt()
          demoDivider.layoutParams = dividerParams
          demoDivider.alpha = progress
        }
        preparedInsertion.itemView.requestLayout()
      }
      addListener(
        object : AnimatorListenerAdapter() {
          private var cancelled = false

          override fun onAnimationCancel(animation: Animator) {
            cancelled = true
          }

          override fun onAnimationEnd(animation: Animator) {
            restoreActiveDemoInsertion(insertion)
            if (demoInsertionAnimator === animation) {
              demoInsertionAnimator = null
            }
            if (cancelled || !demoTransitionGate.isCurrent(transitionGeneration)) return
            restoreDemoDividerGeometry()
            list.itemAnimator = demoItemAnimator
            finishDemoHeightAnimationAfterLayout(transitionGeneration)
          }
        }
      )
      start()
    }
  }

  private fun animateSingleDemoRemovalIfPossible(
    currentItems: List<LibReference>,
    nextState: LibReferenceMenuBottomSheetState,
    transitionGeneration: Int
  ): Boolean {
    val plan = planLibReferenceDemoUpdate(currentItems, nextState.demoItems)
    if (plan !is LibReferenceDemoUpdatePlan.AnimateRemoval) return false
    val removedHolder = list.findViewHolderForAdapterPosition(plan.removedIndex)
      ?: return false
    val removedView = removedHolder.itemView
    if (removedView.height <= 0) return false

    setDemoHeightAnimationRunning(true)
    removedHolder.setIsRecyclable(false)
    val removedParams = removedView.layoutParams
    val removedStartHeight = removedView.height
    val removedMarginParams = removedParams as? ViewGroup.MarginLayoutParams
    val removedStartTopMargin = removedMarginParams?.topMargin ?: 0
    val removedStartBottomMargin = removedMarginParams?.bottomMargin ?: 0
    val collapseDivider = nextState.demoItems.isEmpty()
    val dividerParams = demoDivider.layoutParams as ViewGroup.MarginLayoutParams
    val dividerStartHeight = demoDivider.height
    val dividerStartTopMargin = dividerParams.topMargin
    val dividerStartBottomMargin = dividerParams.bottomMargin
    val removal = ActiveDemoRemoval(
      holder = removedHolder,
      itemView = removedView,
      itemParams = removedParams,
      startTopMargin = removedStartTopMargin,
      startBottomMargin = removedStartBottomMargin
    )
    activeDemoRemoval = removal

    demoRemovalAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
      duration = demoMotionDuration
      interpolator = FastOutSlowInInterpolator()
      addUpdateListener { animator ->
        val remainingFraction = 1f - animator.animatedFraction
        removedParams.height = (removedStartHeight * remainingFraction).toInt()
        removedMarginParams?.topMargin = (removedStartTopMargin * remainingFraction).toInt()
        removedMarginParams?.bottomMargin = (removedStartBottomMargin * remainingFraction).toInt()
        removedView.layoutParams = removedParams
        removedView.alpha = remainingFraction
        if (collapseDivider) {
          dividerParams.height = (dividerStartHeight * remainingFraction).toInt()
          dividerParams.topMargin = (dividerStartTopMargin * remainingFraction).toInt()
          dividerParams.bottomMargin = (dividerStartBottomMargin * remainingFraction).toInt()
          demoDivider.layoutParams = dividerParams
          demoDivider.alpha = remainingFraction
        }
        removedView.requestLayout()
      }
      addListener(
        object : AnimatorListenerAdapter() {
          private var cancelled = false

          override fun onAnimationCancel(animation: Animator) {
            cancelled = true
          }

          override fun onAnimationEnd(animation: Animator) {
            if (demoRemovalAnimator === animation) {
              demoRemovalAnimator = null
            }
            if (!cancelled && demoTransitionGate.isCurrent(transitionGeneration)) {
              list.itemAnimator = null
              renderDemoDivider(nextState)
              demoAdapter.setDiffNewData(nextState.demoItems) {
                if (!demoTransitionGate.isCurrent(transitionGeneration)) return@setDiffNewData
                list.doOnNextLayout {
                  if (!demoTransitionGate.isCurrent(transitionGeneration)) return@doOnNextLayout
                  restoreActiveDemoRemoval(removal)
                  list.itemAnimator = demoItemAnimator
                  restoreDemoDividerGeometry()
                  finishDemoHeightAnimationAfterLayout(transitionGeneration)
                }
              }
            } else {
              restoreActiveDemoRemoval(removal)
            }
          }
        }
      )
      start()
    }
    return true
  }

  private fun renderDemoDivider(state: LibReferenceMenuBottomSheetState) {
    demoDivider.visibility = if (state.demoItems.isEmpty()) View.GONE else View.VISIBLE
  }

  private fun renderOptionButtons(state: LibReferenceMenuBottomSheetState) {
    optionsLayout.removeAllViews()
    state.options.forEach { item ->
      optionsLayout.addView(
        MenuOptionItemView(
          context = context,
          startMarginDp = 4,
          endMarginDp = 4
        ).apply {
          bind(item) { isChecked ->
            this@LibReferenceMenuBSDView.onAction(
              LibReferenceMenuAction.OptionChanged(
                item = item,
                isChecked = isChecked
              )
            )
          }
        }
      )
    }
  }

  private fun cancelOngoingDemoTransition() {
    pendingDemoInsertion = null
    preparedDemoInsertion?.let(::restorePreparedDemoInsertion)
    preparedDemoInsertion = null
    demoInsertionAnimator?.cancel()
    demoInsertionAnimator = null
    activeDemoInsertion?.let(::restoreActiveDemoInsertion)
    activeDemoInsertion = null
    demoRemovalAnimator?.cancel()
    demoRemovalAnimator = null
    activeDemoRemoval?.let(::restoreActiveDemoRemoval)
    activeDemoRemoval = null
    heightSpring.cancel()
    restoreWrapContentHeight()
    restoreDemoDividerGeometry()
    list.itemAnimator = demoItemAnimator
  }

  private fun restorePreparedDemoInsertion(insertion: PreparedDemoInsertion) {
    val marginParams = insertion.itemParams as? ViewGroup.MarginLayoutParams
    insertion.itemParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
    marginParams?.topMargin = insertion.targetTopMargin
    marginParams?.bottomMargin = insertion.targetBottomMargin
    insertion.itemView.layoutParams = insertion.itemParams
    insertion.itemView.alpha = 1f
  }

  private fun restoreActiveDemoInsertion(insertion: ActiveDemoInsertion) {
    restorePreparedDemoInsertion(insertion.preparedInsertion)
    if (insertion.isRecyclabilityLocked) {
      insertion.holder?.setIsRecyclable(true)
      insertion.isRecyclabilityLocked = false
    }
    if (activeDemoInsertion === insertion) {
      activeDemoInsertion = null
    }
  }

  private fun restoreActiveDemoRemoval(removal: ActiveDemoRemoval) {
    val marginParams = removal.itemParams as? ViewGroup.MarginLayoutParams
    removal.itemParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
    marginParams?.topMargin = removal.startTopMargin
    marginParams?.bottomMargin = removal.startBottomMargin
    removal.itemView.layoutParams = removal.itemParams
    removal.itemView.alpha = 1f
    if (removal.isRecyclabilityLocked) {
      removal.holder.setIsRecyclable(true)
      removal.isRecyclabilityLocked = false
    }
    if (activeDemoRemoval === removal) {
      activeDemoRemoval = null
    }
  }

  private fun restoreDemoDividerGeometry() {
    val dividerParams = demoDivider.layoutParams as ViewGroup.MarginLayoutParams
    dividerParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
    dividerParams.topMargin = demoDividerMargin
    dividerParams.bottomMargin = demoDividerMargin
    demoDivider.layoutParams = dividerParams
    demoDivider.alpha = 1f
  }

  private fun demoItemsChanged(items: List<LibReference>): Boolean {
    val currentItems = demoAdapter.data.filterIsInstance<LibReference>()
    if (currentItems.size != items.size) return true
    return currentItems.zip(items).any { (current, next) ->
      current.libName != next.libName || current.type != next.type
    }
  }

  private fun freezeCurrentHeight() {
    if (!heightSpring.isRunning) {
      setDemoHeightAnimationRunning(true)
    }
    layoutParams = layoutParams?.apply {
      height = this@LibReferenceMenuBSDView.height
    }
  }

  private fun animateToMeasuredContentHeight(generation: Int) {
    if (!demoTransitionGate.isCurrent(generation)) return
    val availableHeight = rootView.height.takeIf { it > 0 }
      ?: resources.displayMetrics.heightPixels
    measure(
      View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(availableHeight, View.MeasureSpec.AT_MOST)
    )
    val targetHeight = measuredHeight
    if (targetHeight == height) {
      heightSpring.cancel()
      restoreWrapContentHeight()
      finishDemoHeightAnimationAfterLayout(generation)
      return
    }
    activeHeightSpringGeneration = generation
    if (heightSpring.isRunning) {
      heightSpring.animateToFinalPosition(targetHeight.toFloat())
    } else {
      heightSpring.setStartValue(height.toFloat())
      heightSpring.animateToFinalPosition(targetHeight.toFloat())
    }
  }

  private fun scheduleMeasuredContentHeightAnimation(
    generation: Int,
    delayMillis: Long
  ) {
    val action = Runnable {
      if (demoTransitionGate.isCurrent(generation)) {
        animateToMeasuredContentHeight(generation)
      }
    }
    if (delayMillis > 0L) {
      postDelayed(action, delayMillis)
    } else {
      action.run()
    }
  }

  private fun restoreWrapContentHeight() {
    layoutParams?.let { params ->
      params.height = LayoutParams.WRAP_CONTENT
      layoutParams = params
    }
  }

  private fun finishDemoHeightAnimationAfterLayout(generation: Int) {
    doOnNextLayout {
      if (
        demoTransitionGate.isCurrent(generation) &&
        demoInsertionAnimator == null &&
        demoRemovalAnimator == null &&
        !heightSpring.isRunning
      ) {
        completeDemoTransition(generation)
      }
    }
  }

  private fun completeDemoTransition(generation: Int) {
    if (!demoTransitionGate.isCurrent(generation)) return
    val pendingBind = demoTransitionQueue.complete()
    if (pendingBind == null) {
      setDemoHeightAnimationRunning(false)
    } else {
      post {
        if (demoTransitionGate.isCurrent(generation)) {
          bind(pendingBind.state, pendingBind.onAction)
        }
      }
    }
  }

  private fun setDemoHeightAnimationRunning(running: Boolean) {
    if (isDemoHeightAnimationRunning == running) return
    isDemoHeightAnimationRunning = running
    onDemoHeightAnimationStateChange(running)
  }

  private inner class OptionsAdapter : BaseQuickAdapter<Unit, BaseViewHolder>(0) {

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
      return BaseViewHolder(
        LinearLayout(context).apply {
          layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
          )
          orientation = VERTICAL
        }
      )
    }

    override fun convert(holder: BaseViewHolder, item: Unit) {
      (holder.itemView as LinearLayout).setSingleChild(optionsContainer)
    }
  }

  private data class PreparedDemoInsertion(
    val itemView: View,
    val itemParams: ViewGroup.LayoutParams,
    val targetHeight: Int,
    val targetTopMargin: Int,
    val targetBottomMargin: Int
  )

  private data class PendingDemoInsertion(
    val identity: Pair<String, Int>,
    val generation: Int
  )

  private data class PendingDemoBind(
    val state: LibReferenceMenuBottomSheetState,
    val onAction: (LibReferenceMenuAction) -> Unit
  )

  private data class ActiveDemoInsertion(
    val preparedInsertion: PreparedDemoInsertion,
    val holder: RecyclerView.ViewHolder?,
    var isRecyclabilityLocked: Boolean = false
  )

  private data class ActiveDemoRemoval(
    val holder: RecyclerView.ViewHolder,
    val itemView: View,
    val itemParams: ViewGroup.LayoutParams,
    val startTopMargin: Int,
    val startBottomMargin: Int,
    var isRecyclabilityLocked: Boolean = true
  )

  private companion object {
    const val DEFAULT_DEMO_MOTION_DURATION = 350
    const val DEFAULT_DEMO_EFFECT_DURATION = 150

    val HEIGHT_PROPERTY = object : FloatPropertyCompat<LibReferenceMenuBSDView>("height") {
      override fun getValue(view: LibReferenceMenuBSDView): Float = view.height.toFloat()

      override fun setValue(view: LibReferenceMenuBSDView, value: Float) {
        view.layoutParams = view.layoutParams?.apply {
          height = value.toInt()
        }
      }
    }
  }
}
