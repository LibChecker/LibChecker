package com.absinthe.libchecker.features.applist.detail.ui.adapter

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.TransitionDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.text.LineBreaker
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.ViewGroup
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.text.set
import androidx.core.text.strikeThrough
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ET_DYN
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_TEXT
import com.absinthe.libchecker.features.applist.detail.ui.XmlBSDFragment
import com.absinthe.libchecker.features.applist.detail.ui.view.ComponentLibItemView
import com.absinthe.libchecker.features.applist.detail.ui.view.MetadataLibItemView
import com.absinthe.libchecker.features.applist.detail.ui.view.NativeLibItemView
import com.absinthe.libchecker.features.applist.detail.ui.view.StaticLibItemView
import com.absinthe.libchecker.features.statistics.bean.DISABLED
import com.absinthe.libchecker.features.statistics.bean.EXPORTED
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.ui.adapter.HighlightAdapter
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.tintTextToPrimary
import com.absinthe.libchecker.utils.manifest.ResourceParser
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import timber.log.Timber

private const val HIGHLIGHT_TRANSITION_DURATION = 250

class LibStringAdapter(
  val packageName: String,
  @LibType val type: Int,
  private val fragmentManager: FragmentManager? = null
) : HighlightAdapter<LibStringItemChip>() {

  var highlightPosition: Int = -1
    private set

  var processMap: Map<String, Int> = mapOf()

  private val appResources by lazy {
    runCatching {
      context.packageManager.getResourcesForApplication(packageName)
    }.getOrNull()
  }

  private var processMode: Boolean = false
  private var is64Bit: Boolean = false

  fun switchProcessMode() {
    processMode = !processMode
    //noinspection NotifyDataSetChanged
    notifyDataSetChanged()
  }

  fun setProcessMode(isProcessMode: Boolean) {
    processMode = isProcessMode
    //noinspection NotifyDataSetChanged
    notifyDataSetChanged()
  }

  fun set64Bit(is64Bit: Boolean) {
    this.is64Bit = is64Bit
    //noinspection NotifyDataSetChanged
    notifyDataSetChanged()
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return when (type) {
      NATIVE -> createBaseViewHolder(NativeLibItemView(context))
      METADATA -> createBaseViewHolder(MetadataLibItemView(context))
      STATIC -> createBaseViewHolder(StaticLibItemView(context))
      else -> createBaseViewHolder(ComponentLibItemView(context))
    }
  }

  override fun convert(holder: BaseViewHolder, item: LibStringItemChip) {
    val itemName = when (item.item.source) {
      DISABLED -> {
        if ((GlobalValues.itemAdvancedOptions and AdvancedOptions.MARK_DISABLED) > 0 || type == PERMISSION) {
          buildSpannedString {
            strikeThrough {
              inSpans(StyleSpan(Typeface.BOLD_ITALIC)) {
                append(item.item.name)
              }
            }
            // prevent text clipping
            append(" ")
          }
        } else {
          item.item.name
        }
      }

      EXPORTED -> {
        if ((GlobalValues.itemAdvancedOptions and AdvancedOptions.MARK_EXPORTED) > 0) {
          buildSpannedString {
            append(item.item.name)
            setSpan(
              ForegroundColorSpan(context.getColorByAttr(com.google.android.material.R.attr.colorPrimary)),
              0,
              item.item.name.length,
              Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
          }
        } else {
          item.item.name
        }
      }

      else -> {
        item.item.name
      }
    }

    when (type) {
      NATIVE -> setNativeContent(holder.itemView as NativeLibItemView, item, itemName)

      PERMISSION -> setPermissionContent(holder.itemView as ComponentLibItemView, item, itemName)

      METADATA -> setMetadataContent(holder.itemView as MetadataLibItemView, item, itemName)

      STATIC -> setStaticContent(holder.itemView as StaticLibItemView, item, itemName)

      else -> {
        (holder.itemView as ComponentLibItemView).apply {
          processLabelColor = if (item.item.process.isNullOrEmpty() || !processMode) {
            -1
          } else {
            processMap[item.item.process] ?: UiUtils.getRandomColor()
          }
          setOrHighlightText(libName, itemName)
          if ((GlobalValues.itemAdvancedOptions and AdvancedOptions.SHOW_MARKED_LIB) > 0) {
            setChip(item.rule)
          } else {
            setChip(null)
          }
        }
      }
    }

    if (highlightPosition == -1 || holder.absoluteAdapterPosition != highlightPosition) {
      if (holder.itemView.background is TransitionDrawable) {
        (holder.itemView.background as TransitionDrawable).reverseTransition(
          HIGHLIGHT_TRANSITION_DURATION
        )
      }
      holder.itemView.background = null
    } else {
      val drawable = TransitionDrawable(
        listOf(
          ColorDrawable(Color.TRANSPARENT),
          ColorDrawable(R.color.highlight_component.getColor(context))
        ).toTypedArray()
      )
      holder.itemView.background = drawable
      if (holder.itemView.background is TransitionDrawable) {
        (holder.itemView.background as TransitionDrawable).startTransition(
          HIGHLIGHT_TRANSITION_DURATION
        )
      }
    }
  }

  fun setHighlightBackgroundItem(position: Int) {
    if (position < 0) {
      return
    }
    highlightPosition = position
  }

  private fun setNativeContent(
    itemView: NativeLibItemView,
    item: LibStringItemChip,
    itemName: CharSequence
  ) {
    itemView.processLabelColor = if (item.item.process.isNullOrEmpty() || !processMode) {
      -1
    } else {
      processMap[item.item.process] ?: UiUtils.getRandomColor()
    }
    setOrHighlightText(itemView.libName, itemName)
    itemView.libSize.text = PackageUtils.sizeToString(context, item.item, showElfInfo = true, is64Bit = is64Bit)
    if ((GlobalValues.itemAdvancedOptions and AdvancedOptions.SHOW_MARKED_LIB) > 0) {
      itemView.setChip(item.rule)
    } else {
      itemView.setChip(null)
    }

    if (item.item.elfType != ET_DYN) {
      itemView.libName.tintTextToPrimary()
      itemView.libSize.tintTextToPrimary()
    }
  }

  private fun setStaticContent(
    itemView: StaticLibItemView,
    item: LibStringItemChip,
    itemName: CharSequence
  ) {
    setOrHighlightText(itemView.libName, itemName)
    itemView.libDetail.let {
      if (OsUtils.atLeastQ()) {
        it.breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
      } else if (OsUtils.atLeastO()) {
        // noinspection WrongConstant
        it.breakStrategy = Layout.BREAK_STRATEGY_SIMPLE
      }
      val sb = SpannableStringBuilder(item.item.source)
      val staticPrefixIndex = sb.indexOf(PackageUtils.STATIC_LIBRARY_SOURCE_PREFIX)
      sb[staticPrefixIndex, staticPrefixIndex + PackageUtils.STATIC_LIBRARY_SOURCE_PREFIX.length] =
        StyleSpan(Typeface.BOLD)

      val versionCodePrefixIndex = sb.indexOf(PackageUtils.VERSION_CODE_PREFIX)
      sb[versionCodePrefixIndex, versionCodePrefixIndex + PackageUtils.VERSION_CODE_PREFIX.length] =
        StyleSpan(Typeface.BOLD)

      it.text = sb
    }
    if ((GlobalValues.itemAdvancedOptions and AdvancedOptions.SHOW_MARKED_LIB) > 0) {
      itemView.setChip(item.rule)
    } else {
      itemView.setChip(null)
    }
  }

  private fun setPermissionContent(
    itemView: ComponentLibItemView,
    item: LibStringItemChip,
    itemName: CharSequence
  ) {
    itemView.processLabelColor = if (item.item.size == 0L) {
      R.color.material_red_500.getColor(context)
    } else {
      -1
    }
    setOrHighlightText(itemView.libName, itemName)
  }

  private val metadataLinkable = setOf("string", "array", "bool", "xml", "drawable", "mipmap", "color", "dimen")

  private fun setMetadataContent(
    itemView: MetadataLibItemView,
    item: LibStringItemChip,
    itemName: CharSequence
  ) {
    setOrHighlightText(itemView.libName, itemName)
    setOrHighlightText(itemView.libSize, item.item.source.orEmpty())

    val type = runCatching {
      appResources?.getResourceTypeName(item.item.size.toInt())
    }.getOrDefault("null")
    itemView.linkToIcon.isVisible = metadataLinkable.contains(type)

    if (itemView.linkToIcon.isVisible) {
      itemView.linkToIcon.setOnClickListener {
        val transformed = itemView.linkToIcon.getTag(R.id.resource_transformed_id) as? Boolean ?: false
        if (transformed) {
          itemView.libSize.text = item.item.source
          itemView.linkToIcon.setImageResource(R.drawable.ic_outline_change_circle_24)
          itemView.linkToIcon.setTag(R.id.resource_transformed_id, false)
        } else {
          var clickedTag = false
          item.item.source?.let {
            Timber.d("type: $type")

            runCatching {
              when (type) {
                "string" -> {
                  appResources?.let { res ->
                    itemView.libSize.text = res.getString(item.item.size.toInt())
                  }
                  clickedTag = true
                }

                "array" -> {
                  appResources?.let { res ->
                    itemView.libSize.text =
                      res.getStringArray(item.item.size.toInt()).contentToString()
                  }
                  clickedTag = true
                }

                "bool" -> {
                  appResources?.let { res ->
                    itemView.libSize.text = res.getBoolean(item.item.size.toInt()).toString()
                  }
                }

                "xml" -> {
                  fragmentManager?.let { fm ->
                    appResources?.let { res ->
                      res.getXml(item.item.size.toInt()).let {
                        val text = ResourceParser(it).setMarkColor(true).parse()
                        XmlBSDFragment().apply {
                          arguments = bundleOf(
                            EXTRA_TEXT to text
                          )
                          show(fm, XmlBSDFragment::class.java.name)
                        }
                      }
                    }
                  }
                  clickedTag = false
                }

                "drawable", "mipmap" -> {
                  appResources?.getDrawable(item.item.size.toInt(), null)?.let { drawable ->
                    val bitmap = drawable.toBitmap(
                      itemView.linkToIcon.measuredWidth,
                      itemView.linkToIcon.measuredHeight,
                      Bitmap.Config.ARGB_8888
                    )
                    itemView.linkToIcon.load(bitmap)
                  }
                  clickedTag = true
                }

                "color" -> {
                  appResources?.getColor(item.item.size.toInt(), null)?.let { colorInt ->
                    itemView.linkToIcon.load(
                      ShapeDrawable(OvalShape()).apply {
                        paint.color = colorInt
                      }.toBitmap(
                        itemView.linkToIcon.measuredWidth,
                        itemView.linkToIcon.measuredHeight,
                        Bitmap.Config.ARGB_8888
                      )
                    )
                  }
                  clickedTag = true
                }

                "dimen" -> {
                  appResources?.let { res ->
                    itemView.libSize.text = res.getDimension(item.item.size.toInt()).toString()
                  }
                  clickedTag = true
                }

                else -> {
                  clickedTag = false
                }
              }
            }
          }
          itemView.linkToIcon.setTag(R.id.resource_transformed_id, clickedTag)
        }
      }
    }
  }
}
