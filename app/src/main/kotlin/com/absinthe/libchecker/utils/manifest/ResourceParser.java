package com.absinthe.libchecker.utils.manifest;

import android.content.res.XmlResourceParser;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.absinthe.libchecker.utils.UiUtils;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class ResourceParser {

  private static final int COLOR_TAG = UiUtils.INSTANCE.getRandomColor();
  private static final int COLOR_ATTR_PREFIX = UiUtils.INSTANCE.getRandomColor();
  private static final int COLOR_ATTR_NAME = UiUtils.INSTANCE.getRandomColor();
  private static final int COLOR_ATTR_VALUE = UiUtils.INSTANCE.getRandomColor();
  private static final int COLOR_COMMENT = UiUtils.INSTANCE.getRandomColor();
  /* 命名空间 */
  private static final String NAMESPACE_ANDROID = "http://schemas.android.com/apk/res/android";
  private static final String NAMESPACE_TOOLS = "http://schemas.android.com/tools";
  private static final String NAMESPACE_APP = "http://schemas.android.com/apk/res-auto";

  private static final Map<String, String> mNamespaces = new HashMap<>();

  private boolean lineSpace = false; // 行间距
  private boolean markColor = false; // 着色

  private final XmlResourceParser mParser;

  public ResourceParser(XmlResourceParser parser) {
    this.mParser = parser;
    init();
  }

  private void init() {
    mNamespaces.put(NAMESPACE_ANDROID, "android");
    mNamespaces.put(NAMESPACE_TOOLS, "tools");
    mNamespaces.put(NAMESPACE_APP, "app");
  }

  /**
   * 解析apk文件
   */
  public CharSequence parse() {
    try {
      // 保存输出的xml文本
      StringBuilder builder = new StringBuilder();
      // 生成着色字符位置列表
      List<SpanText> spanTexts = new ArrayList<>();
      // 第一行
      // builder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
      // 创建一个节点树
      Map<String, Node> nodes = new HashMap<>();
      Node lastNode = null;
      int level = 0;
      int event = mParser.getEventType();

      while (event != XmlPullParser.END_DOCUMENT) {
        switch (event) {
          case XmlPullParser.START_DOCUMENT -> Timber.i("START_DOCUMENT");
          case XmlPullParser.START_TAG -> {
            if (lastNode != null && lastNode.isTagOpen) {
              // 关闭上一个标签开头
              spanTexts.add(new SpanText(builder.length(), builder.length() + 1, COLOR_TAG));
              builder.append(">");
              lastNode.hasSubTag = true;
            }
            level++;
            // 保存节点
            Node node = new Node(mParser.getName(),
              mParser.getDepth(),
              mParser.isEmptyElementTag(),
              mParser.getAttributeCount());
            lastNode = node;
            nodes.put(mParser.getName() + '\0' + mParser.getDepth(), node);
            if (builder.length() > 0) {
              builder.append('\n')
                .append(makeIndent(level));
            }
            spanTexts.add(new SpanText(builder.length(), builder.length() + 1 + node.name.length(), COLOR_TAG));
            builder.append('<')
              .append(node.name);
            int attrCount = node.attrCount;
            if (attrCount > 0) {
              // 解析属性
              if (attrCount == 1) {
                builder.append(' ');
              } else {
                builder.append('\n');
              }
              for (int i = 0; i < attrCount; i++) {
                String name = mParser.getAttributeName(i); // 属性名称
                String value = mParser.getAttributeValue(i); // 属性值
                String namespace = mParser.getAttributeNamespace(i); // 命名空间
                if (attrCount > 1) {
                  builder.append(makeIndent(level + 1));
                }
                String prefix = mNamespaces.get(namespace); // 命名空间前缀
                if (prefix != null) {
                  spanTexts.add(new SpanText(builder.length(), builder.length() + prefix.length() + 1, COLOR_ATTR_PREFIX));
                  builder.append(prefix).append(":");
                }
                spanTexts.add(new SpanText(builder.length(), builder.length() + name.length() + 1, COLOR_ATTR_NAME));
                builder.append(name).append('=');
                spanTexts.add(new SpanText(builder.length(), builder.length() + ("\"" + value + "\"").length(), COLOR_ATTR_VALUE));
                builder.append('\"').append(value).append('\"');
                if (attrCount == 1) {
                  // builder.append(' ');
                } else if (i != attrCount - 1) {
                  builder.append('\n');
                }
              }
            }
            if (node.isEmptyTag) {
              // 标签没有元素,关闭
              spanTexts.add(new SpanText(builder.length(), builder.length() + 2, COLOR_TAG));
              builder.append("/>");
              node.isTagOpen = false;
              node.hasSubTag = false;
              if (lineSpace) builder.append('\n');
            }
          }
          case XmlPullParser.TEXT -> {
            if (lastNode != null) {
              spanTexts.add(new SpanText(builder.length(), builder.length() + 1, COLOR_TAG));
              builder.append('>');
              if (!mParser.isWhitespace()) {
                builder.append("\n").append(makeIndent(level + 1)).append(mParser.getText());
              }
              if (lastNode.attrCount > 1) builder.append('\n');
              lastNode.hasText = true;
            }
          }
          case XmlPullParser.END_TAG -> {
            Node node2 = nodes.get(mParser.getName() + '\0' + mParser.getDepth()); //获取标签开始
            if (node2 != null && node2.isTagOpen) {
              if (node2.hasSubTag || node2.hasText) {
                // 关闭双标记
                builder.append('\n')
                  .append(makeIndent(level));
                spanTexts.add(new SpanText(builder.length(), builder.length() + 3 + mParser.getName().length(), COLOR_TAG));
                builder.append("</")
                  .append(mParser.getName())
                  .append(">");
              } else {
                // 关闭单标记
                spanTexts.add(new SpanText(builder.length() + 1, builder.length() + 3, COLOR_TAG));
                builder.append(" />");
              }
              if (lineSpace) builder.append('\n');
              node2.isTagOpen = false;
            }
            level--;
          }
          case XmlPullParser.COMMENT -> {
            // 注释
            builder.append('\n')
              .append(makeIndent(level + 1));
            spanTexts.add(new SpanText(builder.length(), builder.length() + 9 + mParser.getText().length(), COLOR_COMMENT));
            builder.append("<!-- ")
              .append(mParser.getText())
              .append(" -->");
          }
          case XmlPullParser.CDSECT -> Timber.i("CDATA: %s", mParser.getText());
          default -> Timber.i("event: %s", event);
        }
        event = mParser.nextToken();
      }
      // 代码上色
      if (markColor) {
        SpannableString string = new SpannableString(builder);
        for (SpanText spanText : spanTexts) {
          string.setSpan(new ForegroundColorSpan(spanText.color), spanText.start, spanText.end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        }
        return string;
      } else {
        return builder;
      }
    } catch (Exception e) {
      Timber.e(e.toString());
    } finally {
      if (mParser != null) {
        mParser.close(); // 关闭流，最终都会执行
      }
    }
    return null;
  }


  /**
   * 生成缩进
   */
  private String makeIndent(int level) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < (level - 1) * 4; i++) {
      b.append(' ');
    }
    return b.toString();
  }

  /**
   * 自定义节点类
   */
  private static class Node {
    final String name;  // 名称
    final int depth; // 深度
    final boolean isEmptyTag; // 是单标签
    final int attrCount; // 属性个数
    boolean isTagOpen = true;
    boolean hasSubTag;
    boolean hasText;

    public Node(String name, int depth, boolean isEmptyTag, int attrCount) {
      this.name = name;
      this.depth = depth;
      this.isEmptyTag = isEmptyTag;
      this.attrCount = attrCount;
    }
  }

  /**
   * 设置是否显示行间距
   */
  public ResourceParser setLineSpace(boolean lineSpace) {
    this.lineSpace = lineSpace;
    return this;
  }

  /**
   * 设置是否标记颜色
   */
  public ResourceParser setMarkColor(boolean markColor) {
    this.markColor = markColor;
    return this;
  }

  /**
   * 此类保存上色文本信息
   */
  private record SpanText(int start, int end, int color) {
  }
}
