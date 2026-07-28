package com.absinthe.libchecker.domain.statistics.chart.usecase

class ValidateStatisticSvgUseCase {

  operator fun invoke(bytes: ByteArray): List<String> {
    if (bytes.size > MAX_SVG_BYTES) {
      return listOf("SVG exceeds $MAX_SVG_BYTES bytes")
    }
    val svg = bytes.toString(Charsets.UTF_8)
    val errors = mutableListOf<String>()
    val normalized = svg.lowercase()

    FORBIDDEN_CONTENT.firstOrNull { it in normalized }?.let {
      errors += "SVG contains forbidden content: $it"
    }
    val tags = TAG.findAll(svg).toList()
    if (tags.size > MAX_TAG_COUNT) {
      errors += "SVG contains too many elements"
    }
    tags.forEach { match ->
      val tagName = match.groupValues[1].lowercase()
      if (tagName !in ALLOWED_TAGS) {
        errors += "SVG contains unsupported element: $tagName"
      }
    }
    if (EVENT_ATTRIBUTE.containsMatchIn(svg)) {
      errors += "SVG contains an event attribute"
    }
    val svgStart = SVG_START.find(svg)
    if (svgStart == null) {
      errors += "SVG root element is missing"
    } else {
      val viewBox = VIEW_BOX.find(svgStart.value)?.groupValues?.get(1)
      if (viewBox == null || !isValidViewBox(viewBox)) {
        errors += "SVG viewBox is missing or invalid"
      }
    }
    return errors.distinct()
  }

  private fun isValidViewBox(viewBox: String): Boolean {
    val values = viewBox.trim().split(VIEW_BOX_SEPARATOR)
      .filter { it.isNotEmpty() }
      .mapNotNull { it.toDoubleOrNull() }
    if (values.size != 4) return false
    val width = values[2]
    val height = values[3]
    if (!width.isFinite() || !height.isFinite() || width <= 0 || height <= 0) return false
    if (width > MAX_VIEW_BOX_SIZE || height > MAX_VIEW_BOX_SIZE) return false
    val aspectRatio = width / height
    return aspectRatio in MIN_ASPECT_RATIO..MAX_ASPECT_RATIO
  }

  companion object {
    const val MAX_SVG_BYTES = 64 * 1024
    private const val MAX_TAG_COUNT = 512
    private const val MAX_VIEW_BOX_SIZE = 4096.0
    private const val MIN_ASPECT_RATIO = 0.5
    private const val MAX_ASPECT_RATIO = 2.0
    private val ALLOWED_TAGS = setOf("svg", "g", "path", "circle", "rect", "line", "polyline", "polygon", "ellipse")
    private val FORBIDDEN_CONTENT = listOf(
      "<!doctype",
      "<!entity",
      "<?xml-stylesheet",
      "<script",
      "<foreignobject",
      "<image",
      "<style",
      "<text",
      "href=",
      "xlink:",
      "url("
    )
    private val TAG = Regex("<\\s*/?\\s*([A-Za-z][A-Za-z0-9:_-]*)\\b")
    private val SVG_START = Regex("<\\s*svg\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val VIEW_BOX = Regex("\\bviewBox\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
    private val EVENT_ATTRIBUTE = Regex("\\son[a-z]+\\s*=", RegexOption.IGNORE_CASE)
    private val VIEW_BOX_SEPARATOR = Regex("[\\s,]+")
  }
}
