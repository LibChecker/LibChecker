package com.absinthe.libchecker.utils.manifest

import android.R.attr
import android.content.res.XmlResourceParser
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.absinthe.libchecker.utils.UiUtils
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber

class ResourceParser(private val xmlResourceParser: XmlResourceParser?) {

    private var lineSpace = false // 行间距

    private var markColor = false // 着色

    init {
        namespaces[NAMESPACE_ANDROID] = "android"
        namespaces[NAMESPACE_TOOLS] = "tools"
        namespaces[NAMESPACE_APP] = "app"
    }

    fun parse(): CharSequence? {
        try {
            // 保存输出的xml文本

            // 保存输出的xml文本
            val builder = java.lang.StringBuilder()
            // 生成着色字符位置列表
            // 生成着色字符位置列表
            val spanTexts = ArrayList<SpanText>()
            // 第一行
            // builder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            // 创建一个节点树
            // 第一行
            // builder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            // 创建一个节点树
            val nodes: MutableMap<String, Node> = HashMap()
            var lastNode: Node? = null
            var level: Int = 0
            var event: Int = xmlResourceParser!!.getEventType()

            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_DOCUMENT -> {
                        Timber.i("START_DOCUMENT")
                    }
                    XmlPullParser.START_TAG -> {
                        if (lastNode != null && lastNode.isTagOpen) {
                            // 关闭上一个标签开头

                            // 关闭上一个标签开头
                            spanTexts.add(SpanText(builder.length, builder.length + 1, COLOR_TAG))
                            // builder.append(">\n");
                            // builder.append(">\n");
                            lastNode.hasSubTag = true
                        }
                        level++

                        val node = Node(xmlResourceParser.name,
                                        xmlResourceParser.depth,
                                        xmlResourceParser.isEmptyElementTag,
                                        xmlResourceParser.attributeCount
                        )
                        lastNode = node

                        nodes[xmlResourceParser.name + 0x00 + xmlResourceParser.depth] = node
                        if (builder.length > 0) {
                            builder.append('\n')
                            .append(makeIndent(level))
                        }

                        spanTexts.add(
                            SpanText(
                                builder.length,
                                builder.length + 1 + node.name.length,
                                COLOR_TAG
                            )
                        )
                        builder.append('<')
                          .append(node.name)

                        val attrCount = node.attrCount
                        if (attrCount > 0) {
                            if (attrCount == 1) {
                                builder.append(' ')
                            } else {
                                builder.append('\n')
                            }
                            for (i in 0 until attrCount) {
                                val name: String = xmlResourceParser.getAttributeName(i) // 属性名称
                                val value: String = xmlResourceParser.getAttributeValue(i) // 属性值
                                val namespace: String = xmlResourceParser.getAttributeNamespace(i) // 命名空间

                                if (attrCount > 1) {
                                    builder.append(makeIndent(attr.level + 1))
                                }

                                val prefix: String? = namespaces.get(namespace) // 命名空间前缀

                                if (prefix != null) {
                                    spanTexts.add(
                                      SpanText(
                                        builder.length,
                                        builder.length + prefix.length + 1,
                                        COLOR_ATTR_PREFIX
                                      )
                                    )
                                    builder.append(prefix).append(":")
                                }

                                spanTexts.add(
                                    SpanText(
                                      builder.length,
                                      builder.length + name.length + 1,
                                      COLOR_ATTR_NAME
                                    )
                                )

                                builder.append(name).append('=')
                                spanTexts.add(
                                    SpanText(
                                      builder.length,
                                      builder.length + ("\"" + value + "\"").length,
                                      COLOR_ATTR_VALUE
                                    )
                                )

                                builder.append('\"').append(value).append('\"')
                                if (attrCount === 1) {
                                  // builder.append(' ');
                                } else if (i != attrCount - 1) {
                                    builder.append('\n')
                                }
                            }

                        }

                        if (node.isEmptyTag) {
                            spanTexts.add(
                              SpanText(
                                  builder.length,
                                  builder.length + 2,
                                  COLOR_TAG
                              )
                            )
                            builder.append("/>")
                            node.isTagOpen = false
                            node.hasSubTag = false
                            if (lineSpace) builder.append('\n')
                        }
                    }
                    XmlPullParser.TEXT -> {
                      if (lastNode != null) {
                          spanTexts.add(SpanText(builder.length, builder.length + 1, COLOR_TAG))
                          builder.append('>')
                          if (!xmlResourceParser.isWhitespace) {
                              builder.append("\n").append(makeIndent(attr.level + 1))
                                .append(xmlResourceParser.text)
                          }
                          if (lastNode.attrCount > 1) builder.append('\n')
                          lastNode.hasText = true
                      }
                    }
                    XmlPullParser.END_TAG -> {
                        val node2 = nodes[xmlResourceParser.name + '\u0000' + xmlResourceParser.depth] //获取标签开始

                        if (node2 != null && node2.isTagOpen) {
                            if (node2.hasSubTag || node2.hasText) {
                                // 关闭双标记
                                builder.append('\n')
                                  .append(makeIndent(attr.level))
                                spanTexts.add(
                                  SpanText(
                                    builder.length,
                                    builder.length + 3 + xmlResourceParser.name.length,
                                    COLOR_TAG
                                  )
                                )
                                builder.append("</")
                                  .append(xmlResourceParser.name)
                                  .append(">")
                            } else {
                                // 关闭单标记
                                spanTexts.add(
                                  SpanText(
                                    builder.length + 1,
                                    builder.length + 3,
                                    COLOR_TAG
                                  )
                                )
                                builder.append(" />")
                            }
                            if (lineSpace) builder.append('\n')
                            node2.isTagOpen = false
                        }
                        level--
                    }
                    XmlPullParser.COMMENT -> {
                        builder.append('\n').append(makeIndent(level + 1))

                        spanTexts.add(SpanText(
                            builder.length,
                            builder.length + 9 + xmlResourceParser.text.length,
                            COLOR_COMMENT)
                        )

                        builder.append("<!-- ")
                          .append(xmlResourceParser.text)
                          .append(" -->")
                    }
                    XmlPullParser.CDSECT -> {
                        Timber.i("CDATA: ${xmlResourceParser.text}")
                    }
                    else -> {
                        Timber.i("event: $event")
                    }


                }
                event = xmlResourceParser.nextToken()
            }

            if (markColor) {
                val string = SpannableString(builder)
                for (spanText in spanTexts) {
                    string.setSpan(
                        ForegroundColorSpan(spanText.color),
                        spanText.start,
                        spanText.end,
                        Spanned.SPAN_EXCLUSIVE_INCLUSIVE
                    )
                }
                return string
            } else {
                return builder
            }

        } catch (exception: Exception) {
            Timber.e(exception)
        } finally {
            xmlResourceParser?.close()
        }
        return null
    }

    private fun makeIndent(level: Int): String {
        val b = StringBuilder()
        for (i in (0 until (attr.level - 1) * 4)) {
            b.append(' ')
        }
        return b.toString()
    }

    fun setLineSpace(lineSpace: Boolean): ResourceParser {
        this.lineSpace = lineSpace
        return this
    }

    fun setMarkColor(markColor: Boolean): ResourceParser {
        this.markColor = markColor
        return this
    }


    /**
     * 此类保存上色文本信息
     */
    private class SpanText(
        val start: Int,
        val end: Int,
        val color: Int
    )

    private class Node( // 名称
        val name: String,  // 深度
        val depth: Int, isEmptyTag: Boolean, attrCount: Int
    ) {
        val isEmptyTag // 是单标签
          : Boolean
        val attrCount // 属性个数
          : Int
        var isTagOpen = true
        var hasSubTag = false
        var hasText = false

        init {
          this.isEmptyTag = isEmptyTag
          this.attrCount = attrCount
        }
    }

    private companion object {

        val COLOR_TAG: Int = UiUtils.getRandomColor()

        val COLOR_ATTR_PREFIX: Int = UiUtils.getRandomColor()

        val COLOR_ATTR_NAME: Int = UiUtils.getRandomColor()

        val COLOR_ATTR_VALUE: Int = UiUtils.getRandomColor()

        val COLOR_COMMENT: Int = UiUtils.getRandomColor()

        /* 命名空间 */

        const val NAMESPACE_ANDROID = "http://schemas.android.com/apk/res/android"
        const val NAMESPACE_TOOLS = "http://schemas.android.com/tools"
        const val NAMESPACE_APP = "http://schemas.android.com/apk/res-auto"

        val namespaces = HashMap<String, String>()
    }
}
