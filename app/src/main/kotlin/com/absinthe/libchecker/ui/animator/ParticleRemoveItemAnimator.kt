package com.absinthe.libchecker.ui.animator

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

class ParticleRemoveItemAnimator : DefaultItemAnimator() {

  private val pendingParticleRemoveItemIds = linkedSetOf<Long>()
  private val activeParticleDecorations = linkedSetOf<ParticleAnimationDecoration>()

  fun prepareParticleRemovals(itemIds: Collection<Long>) {
    pendingParticleRemoveItemIds.clear()
    pendingParticleRemoveItemIds.addAll(itemIds.take(MAX_PARTICLE_REMOVES_PER_BATCH))
  }

  // The particle effect is remove-only. Add/change animations are left to DefaultItemAnimator.
  override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
    if (!pendingParticleRemoveItemIds.remove(holder.itemId)) {
      return super.animateRemove(holder)
    }

    val itemView = holder.itemView
    val recyclerView = itemView.parent as? RecyclerView ?: return super.animateRemove(holder)
    if (itemView.width <= 0 || itemView.height <= 0 || recyclerView.width <= 0 || recyclerView.height <= 0) {
      return super.animateRemove(holder)
    }

    val bitmap = itemView.createSnapshotOrNull() ?: return super.animateRemove(holder)
    cancelActiveDecorations(holder.itemId)
    dispatchRemoveStarting(holder)
    val decoration = ParticleAnimationDecoration(
      recyclerView = recyclerView,
      bitmap = bitmap,
      originLeft = itemView.left + itemView.translationX,
      originTop = itemView.top + itemView.translationY,
      seed = holder.itemId,
      onStart = {
        holder.itemView.alpha = 0f
      },
      onCleanUp = {
        activeParticleDecorations.remove(it)
        resetView(holder.itemView)
        dispatchRemoveFinished(holder)
      }
    )
    activeParticleDecorations.add(decoration)
    decoration.start()

    return true
  }

  override fun endAnimation(item: RecyclerView.ViewHolder) {
    pendingParticleRemoveItemIds.remove(item.itemId)
    cancelActiveDecorations(item.itemId)
    super.endAnimation(item)
  }

  override fun endAnimations() {
    pendingParticleRemoveItemIds.clear()
    cancelActiveDecorations()
    super.endAnimations()
  }

  private fun cancelActiveDecorations(itemId: Long? = null) {
    activeParticleDecorations
      .filter { itemId == null || it.itemId == itemId }
      .toList()
      .forEach { it.cancel() }
  }

  override fun isRunning(): Boolean {
    return super.isRunning() || activeParticleDecorations.isNotEmpty()
  }

  private fun resetView(view: View) {
    view.alpha = 1f
    view.translationX = 0f
    view.translationY = 0f
    view.translationZ = 0f
  }

  private fun View.createSnapshotOrNull(): Bitmap? {
    return runCatching {
      Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
        draw(Canvas(it))
      }
    }.getOrNull()
  }

  private class ParticleAnimationDecoration(
    private val recyclerView: RecyclerView,
    private val bitmap: Bitmap,
    private val originLeft: Float,
    private val originTop: Float,
    seed: Long,
    private val onStart: () -> Unit,
    private val onCleanUp: (ParticleAnimationDecoration) -> Unit
  ) : RecyclerView.ItemDecoration(),
    View.OnAttachStateChangeListener {

    val itemId = seed
    private val density = recyclerView.resources.displayMetrics.density
    private val particles = createParticles(bitmap, density, seed)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val particlePaint = Paint()
    private val dst = RectF()
    private val startHorizontalScrollOffset = recyclerView.computeHorizontalScrollOffset()
    private val startVerticalScrollOffset = recyclerView.computeVerticalScrollOffset()
    private var progress = 0f
    private var animator: ValueAnimator? = null
    private var cleanedUp = false
    private var started = false
    private var decorationAdded = false
    private val forceCleanUpRunnable = Runnable { cleanUp() }

    fun start() {
      if (cleanedUp || started) {
        return
      }
      if (recyclerView.isComputingLayout) {
        recyclerView.post { start() }
        return
      }
      started = true
      recyclerView.addItemDecoration(this)
      decorationAdded = true
      onStart()
      recyclerView.addOnAttachStateChangeListener(this)
      recyclerView.postDelayed(forceCleanUpRunnable, PARTICLE_DURATION + FORCE_CLEAN_UP_DELAY)
      animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = PARTICLE_DURATION
        interpolator = FastOutSlowInInterpolator()
        addUpdateListener {
          progress = it.animatedValue as Float
          recyclerView.invalidateItemDecorations()
          recyclerView.postInvalidateOnAnimation()
        }
        addListener(object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            cleanUp()
          }

          override fun onAnimationCancel(animation: Animator) {
            cleanUp()
          }
        })
        start()
      }
      recyclerView.invalidateItemDecorations()
      recyclerView.postInvalidateOnAnimation()
    }

    fun cancel() {
      animator?.cancel() ?: cleanUp()
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
      if (bitmap.isRecycled) {
        return
      }

      val drawOriginLeft = originLeft + startHorizontalScrollOffset - parent.computeHorizontalScrollOffset()
      val drawOriginTop = originTop + startVerticalScrollOffset - parent.computeVerticalScrollOffset()

      drawSnapshot(canvas, drawOriginLeft, drawOriginTop)
      for (particle in particles) {
        val localProgress = ((progress - particle.delay) / (1f - particle.delay)).coerceIn(0f, 1f)
        if (localProgress > 0f) {
          particle.drawAnimated(canvas, localProgress, drawOriginLeft, drawOriginTop)
        }
      }
    }

    override fun onViewDetachedFromWindow(v: View) {
      cancel()
    }

    override fun onViewAttachedToWindow(v: View) = Unit

    private fun drawSnapshot(canvas: Canvas, drawOriginLeft: Float, drawOriginTop: Float) {
      val fadeProgress = ((progress - SNAPSHOT_FADE_START) / (SNAPSHOT_FADE_END - SNAPSHOT_FADE_START))
        .coerceIn(0f, 1f)
      val alpha = ((1f - fadeProgress) * START_ALPHA).roundToInt()
      if (alpha <= 0) {
        return
      }

      bitmapPaint.alpha = alpha
      val squeeze = 1f - 0.025f * progress
      val left = drawOriginLeft + bitmap.width * (1f - squeeze) / 2f
      val right = left + bitmap.width * squeeze
      dst.set(left, drawOriginTop, right, drawOriginTop + bitmap.height)
      canvas.drawBitmap(bitmap, null, dst, bitmapPaint)
    }

    private fun Particle.drawAnimated(
      canvas: Canvas,
      localProgress: Float,
      drawOriginLeft: Float,
      drawOriginTop: Float
    ) {
      val eased = 1f - (1f - localProgress) * (1f - localProgress)
      val radius = radius * (1f - 0.45f * eased)
      val centerX = drawOriginLeft + centerX + velocityX * eased + driftX * localProgress * localProgress
      val centerY = drawOriginTop + centerY + velocityY * eased + gravity * localProgress * localProgress
      val fadeIn = (localProgress / PARTICLE_FADE_IN_END).coerceIn(0f, 1f)
      val alpha = (fadeIn * (1f - localProgress) * (1f - localProgress) * START_ALPHA).roundToInt()

      if (alpha <= 0 || radius <= 0f) {
        return
      }

      particlePaint.color = color
      particlePaint.alpha = alpha
      canvas.drawRect(
        centerX - radius,
        centerY - radius,
        centerX + radius,
        centerY + radius,
        particlePaint
      )
    }

    private fun cleanUp() {
      if (cleanedUp) {
        return
      }
      cleanedUp = true
      recyclerView.removeCallbacks(forceCleanUpRunnable)
      animator?.removeAllUpdateListeners()
      animator?.removeAllListeners()
      animator = null
      recyclerView.removeOnAttachStateChangeListener(this)
      if (decorationAdded) {
        runCatching {
          recyclerView.removeItemDecoration(this)
        }
        decorationAdded = false
      }
      recyclerView.invalidateItemDecorations()
      recyclerView.postInvalidateOnAnimation()
      onCleanUp(this)
      recycleBitmap()
    }

    private fun recycleBitmap() {
      if (!bitmap.isRecycled) {
        bitmap.recycle()
      }
    }
  }

  private data class Particle(
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val color: Int,
    val velocityX: Float,
    val velocityY: Float,
    val driftX: Float,
    val gravity: Float,
    val delay: Float
  )

  companion object {
    private const val MAX_PARTICLE_REMOVES_PER_BATCH = 4
    private const val PARTICLE_DURATION = 2200L
    private const val PARTICLE_COUNT_TARGET = 2800
    private const val START_ALPHA = 255
    private const val SNAPSHOT_FADE_START = 0.02f
    private const val SNAPSHOT_FADE_END = 0.24f
    private const val PARTICLE_FADE_IN_END = 0.08f
    private const val FORCE_CLEAN_UP_DELAY = 300L

    fun stableItemIdForKey(key: String): Long {
      return key.hashCode().toLong()
    }

    private fun createParticles(bitmap: Bitmap, density: Float, seed: Long): List<Particle> {
      val random = Random(seed.toInt())
      val minCellSize = (1f * density).roundToInt().coerceAtLeast(1)
      val maxCellSize = (3f * density).roundToInt().coerceAtLeast(minCellSize)
      val areaBasedCellSize = sqrt(bitmap.width * bitmap.height / PARTICLE_COUNT_TARGET.toFloat())
        .roundToInt()
        .coerceIn(minCellSize, maxCellSize)
      val columns = ceil(bitmap.width / areaBasedCellSize.toFloat()).roundToInt().coerceAtLeast(1)
      val rows = ceil(bitmap.height / areaBasedCellSize.toFloat()).roundToInt().coerceAtLeast(1)
      val cellWidth = ceil(bitmap.width / columns.toFloat()).roundToInt().coerceAtLeast(1)
      val cellHeight = ceil(bitmap.height / rows.toFloat()).roundToInt().coerceAtLeast(1)
      val particles = ArrayList<Particle>(columns * rows)

      for (row in 0 until rows) {
        for (column in 0 until columns) {
          val left = column * cellWidth
          val top = row * cellHeight
          val right = (left + cellWidth).coerceAtMost(bitmap.width)
          val bottom = (top + cellHeight).coerceAtMost(bitmap.height)
          if (left >= right || top >= bottom) {
            continue
          }

          val sampleX = ((left + right) / 2).coerceIn(0, bitmap.width - 1)
          val sampleY = ((top + bottom) / 2).coerceIn(0, bitmap.height - 1)
          val color = bitmap.getPixel(sampleX, sampleY)
          if (Color.alpha(color) < 16) {
            continue
          }

          val horizontalProgress = sampleX / bitmap.width.toFloat()
          val verticalProgress = sampleY / bitmap.height.toFloat()
          val outward = horizontalProgress - 0.5f
          val velocityX = (outward * 96f + random.nextFloatIn(-34f, 34f)) * density
          val velocityY = (random.nextFloatIn(-36f, 18f) - verticalProgress * 18f) * density
          val driftX = random.nextFloatIn(-22f, 22f) * density
          val gravity = random.nextFloatIn(42f, 76f) * density
          val delay = (horizontalProgress * 0.16f + verticalProgress * 0.08f + random.nextFloatIn(0f, 0.08f))
            .coerceIn(0f, 0.32f)

          particles += Particle(
            centerX = (left + right) / 2f,
            centerY = (top + bottom) / 2f,
            radius = random.nextFloatIn(
              (cellWidth.coerceAtMost(cellHeight) * 0.28f).coerceAtLeast(1f),
              (cellWidth.coerceAtMost(cellHeight) * 0.5f).coerceAtLeast(1f)
            ),
            color = color,
            velocityX = velocityX,
            velocityY = velocityY,
            driftX = driftX,
            gravity = gravity,
            delay = delay
          )
        }
      }
      return particles
    }

    private fun Random.nextFloatIn(from: Float, until: Float): Float {
      return from + nextFloat() * (until - from)
    }
  }
}
