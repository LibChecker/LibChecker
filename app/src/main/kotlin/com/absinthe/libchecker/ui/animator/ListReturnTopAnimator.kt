package com.absinthe.libchecker.ui.animator

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr

internal fun RecyclerView.createReturnTopAnimator(onEnd: () -> Unit): ValueAnimator {
  stopScroll()
  val travel = 32.dp
  val hostOverlay = (parent as? ViewGroup)?.overlay
  val snapshot = if (hostOverlay != null && width > 0 && height > 0) {
    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
      val canvas = Canvas(it)
      canvas.drawColor(context.getColorByAttr(android.R.attr.colorBackground))
      draw(canvas)
    }
  } else {
    null
  }
  val previous = snapshot?.let { bitmap ->
    BitmapDrawable(resources, bitmap).also {
      it.setBounds(left, top, right, bottom)
      hostOverlay?.add(it)
    }
  }
  positionAtTop(offset = -travel)
  alpha = 0f
  var incomingScroll = 0
  var cancelled = false
  return ValueAnimator.ofFloat(0f, 1f).apply {
    duration = 300
    interpolator = LinearInterpolator()
    addUpdateListener {
      val progress = it.animatedValue as Float
      val outgoing = (progress / 0.6f).coerceIn(0f, 1f)
      val incoming = ((progress - 0.4f) / 0.6f).coerceIn(0f, 1f)
      val outgoingMotion = 1f - (1f - outgoing) * (1f - outgoing)
      val incomingMotion = 1f - (1f - incoming) * (1f - incoming)
      previous?.apply {
        val offset = (travel * outgoingMotion).toInt()
        setBounds(left, top + offset, right, bottom + offset)
        alpha = ((1f - outgoing * outgoing * (3f - 2f * outgoing)) * 255).toInt()
      }
      alpha = incoming * incoming * (3f - 2f * incoming)
      val distance = (travel * incomingMotion).toInt()
      scrollBy(0, incomingScroll - distance)
      incomingScroll = distance
    }
    doOnCancel { cancelled = true }
    doOnEnd {
      if (!cancelled) positionAtTop()
      previous?.let { hostOverlay?.remove(it) }
      alpha = 1f
      snapshot?.recycle()
      onEnd()
    }
  }
}

internal fun RecyclerView.positionAtTop(offset: Int = 0) {
  when (val manager = layoutManager) {
    is LinearLayoutManager -> manager.scrollToPositionWithOffset(0, offset)
    is StaggeredGridLayoutManager -> manager.scrollToPositionWithOffset(0, offset)
    else -> scrollToPosition(0)
  }
}
