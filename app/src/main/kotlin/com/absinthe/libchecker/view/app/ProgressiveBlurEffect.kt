package com.absinthe.libchecker.view.app

import android.graphics.BlendMode
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi

internal const val PROGRESSIVE_BLUR_RADIUS_DP = 10f
internal const val PROGRESSIVE_CURVE = 2.4f
internal const val PROGRESSIVE_SURFACE_TINT_ALPHA = 0.3f

/** The app bar draws these three blurred layers over its backdrop. For a scroll viewport
 * the same transitions include the sharp input and reveal the dialog surface at the edge.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class ProgressiveBlurEffect(radius: Float) {
  private val layers = floatArrayOf(1f, 0.4f, 0.13f).map { fraction ->
    RenderEffect.createBlurEffect(radius * fraction, radius * fraction, Shader.TileMode.CLAMP)
  }
  private val shader = RuntimeShader(LEVEL_MASK)
  private val bands = FloatArray(4)
  private var cached: RenderEffect? = null
  private var cachedSurfaceBlend = false

  fun create(
    start: Float,
    end: Float,
    otherStart: Float = start,
    otherEnd: Float = end,
    blendIntoSurface: Boolean = false
  ): RenderEffect {
    cached?.let {
      if (bands[0] == start && bands[1] == end && bands[2] == otherStart && bands[3] == otherEnd &&
        cachedSurfaceBlend == blendIntoSurface
      ) {
        return it
      }
    }
    bands[0] = start
    bands[1] = end
    bands[2] = otherStart
    bands[3] = otherEnd
    cachedSurfaceBlend = blendIntoSurface
    shader.setFloatUniform("bands", start, end, otherStart, otherEnd)
    shader.setFloatUniform("surfaceBlend", if (blendIntoSurface) 1f else 0f)

    fun mask(level: Int): RenderEffect {
      shader.setFloatUniform("level", level.toFloat())
      return RenderEffect.createRuntimeShaderEffect(shader, "child")
    }

    // With transparent scroll content, crossfade premultiplied samples. Drawing the
    // original underneath a blurred glyph would leave a sharp copy visible through it.
    var stack: RenderEffect? = if (blendIntoSurface) mask(4) else null
    val mode = if (blendIntoSurface) BlendMode.PLUS else BlendMode.SRC_OVER
    for (i in layers.indices.reversed()) {
      val layer = RenderEffect.createChainEffect(mask(i + 1), layers[i])
      stack = stack?.let { RenderEffect.createBlendModeEffect(it, layer, mode) } ?: layer
    }
    return requireNotNull(stack).also { cached = it }
  }
}

private const val LEVEL_MASK = """
uniform shader child;
uniform float4 bands;
uniform float level;
uniform float surfaceBlend;

half4 main(float2 p) {
  float t = clamp(min((p.y - bands.x) / (bands.y - bands.x),
                      (p.y - bands.z) / (bands.w - bands.z)), 0.0, 1.0);
  float ramp = pow(smoothstep(0.0, 1.0, t), $PROGRESSIVE_CURVE);
  float weight = smoothstep(0.0, 1.0, level - 3.0 * ramp);
  if (surfaceBlend > 0.0) {
    weight -= smoothstep(0.0, 1.0, level - 1.0 - 3.0 * ramp);
    weight *= (1.0 - $PROGRESSIVE_SURFACE_TINT_ALPHA * (1.0 - ramp)) * smoothstep(0.0, 0.40, t);
  }
  return child.eval(p) * half(weight);
}
"""
