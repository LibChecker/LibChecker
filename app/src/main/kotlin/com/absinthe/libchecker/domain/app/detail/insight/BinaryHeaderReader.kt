package com.absinthe.libchecker.domain.app.detail.insight

import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightDefinition.Check
import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightDefinition.Layout

internal object BinaryHeaderReader {
  private const val MAX_PREFIX = 512
  private const val MAX_CAPTURE = 128
  private const val MAX_MASK = 80
  private val integer = Regex("[0-9]{1,19}")
  private val field = Regex("(u8|u32le|u32be|u64be)@(end|[0-9]{1,3})")

  fun validLayout(layout: Layout): Boolean = layout.offset in 0 until MAX_PREFIX && layout.checks.size in 1..24 && layout.checks.all { check ->
    check.op in setOf("eq", "ge", "le") && expression(check.left) != null && expression(check.right) != null
  }

  fun validFormat(mask: String): Boolean = maskTokens(mask) != null

  fun read(bytes: ByteArray, size: Long, layouts: List<Layout>, formats: List<String>): String? {
    if (bytes.isEmpty() || bytes.size > MAX_PREFIX || bytes.size > size || layouts.size !in 1..8) return null
    if (formats.size > 8) return null
    val masks = formats.map { maskTokens(it) ?: return null }
    var result: String? = null
    for (layout in layouts) {
      if (!validLayout(layout)) return null
      val start = layout.offset
      val nul = (start until minOf(bytes.size, start + MAX_CAPTURE + 1)).firstOrNull { bytes[it] == 0.toByte() } ?: continue
      if (start == nul || (start until nul).any { bytes[it].toInt() !in 32..126 }) continue
      if (!layout.checks.all { matches(it, bytes, size, nul + 1) }) continue
      val value = String(bytes, start, nul - start, Charsets.US_ASCII)
      if (masks.isNotEmpty() && masks.none { matchesMask(it, value) }) continue
      if (result != null && result != value) return null
      result = value
    }
    return result
  }

  // Each operand has at most one addition. Both terms represent nonnegative signed-Long values.
  private fun expression(text: String): List<String>? {
    if (text.length !in 1..39) return null
    val terms = text.split('+', limit = 3)
    if (terms.size > 2) return null
    return terms.takeIf { tokens ->
      tokens.all { token ->
        when {
          token == "size" || token == "end" -> true
          integer.matches(token) -> token.toLongOrNull() != null
          else -> field.matchEntire(token)?.groupValues?.get(2)?.let { it == "end" || it.toInt() < MAX_PREFIX } == true
        }
      }
    }
  }

  private fun matches(check: Check, bytes: ByteArray, size: Long, end: Int): Boolean {
    fun evaluate(text: String): Long? {
      var sum = 0L
      for (term in expression(text) ?: return null) {
        val value = when (term) {
          "size" -> size
          "end" -> end.toLong()
          else -> term.toLongOrNull() ?: readField(term, bytes, end) ?: return null
        }
        if (Long.MAX_VALUE - sum < value) return null
        sum += value
      }
      return sum
    }
    val left = evaluate(check.left) ?: return false
    val right = evaluate(check.right) ?: return false
    return when (check.op) {
      "eq" -> left == right
      "ge" -> left >= right
      "le" -> left <= right
      else -> false
    }
  }

  private fun readField(term: String, bytes: ByteArray, end: Int): Long? {
    val parts = field.matchEntire(term)?.groupValues ?: return null
    val type = parts[1]
    val offset = if (parts[2] == "end") end else parts[2].toInt()
    val width = when (type) {
      "u8" -> 1
      "u64be" -> 8
      else -> 4
    }
    if (offset > bytes.size - width) return null
    var value = 0L
    for (i in 0 until width) {
      val index = if (type == "u32le") width - 1 - i else i
      value = (value shl 8) or (bytes[offset + index].toLong() and 255)
    }
    return value.takeIf { it >= 0 }
  }

  private fun maskTokens(mask: String): List<String>? {
    if (mask.length !in 1..MAX_MASK || "##" in mask) return null
    val tokens = mutableListOf<String>()
    var i = 0
    while (i < mask.length) {
      val c = mask[i++]
      when {
        c == '[' -> {
          val close = mask.indexOf(']', i)
          if (close <= i) return null
          val letters = mask.substring(i, close)
          if (!letters.all { it in 'a'..'z' || it in 'A'..'Z' }) return null
          tokens += "[$letters]"
          i = close + 1
        }

        c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in "#._-" -> tokens += c.toString()

        else -> return null
      }
    }
    return tokens
  }

  // Track reachable input positions, including all legal lengths of each digit run.
  private fun matchesMask(mask: List<String>, value: String): Boolean {
    var previous = BooleanArray(value.length + 1)
    previous[0] = true
    var next = BooleanArray(previous.size)
    for (token in mask) {
      next[0] = false
      for (i in value.indices) {
        val c = value[i]
        next[i + 1] = when {
          token == "#" -> c in '0'..'9' && (previous[i] || next[i])
          token.startsWith('[') -> previous[i] && token.indexOf(c, 1) in 1 until token.lastIndex
          else -> previous[i] && c == token[0]
        }
      }
      val temp = previous
      previous = next
      next = temp
    }
    return previous.last()
  }
}
