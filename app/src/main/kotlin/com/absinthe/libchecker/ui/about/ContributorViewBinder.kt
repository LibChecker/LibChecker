package com.absinthe.libchecker.ui.about

import android.net.Uri.parse
import android.content.ActivityNotFoundException
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.about.R
import com.drakeet.about.Contributor
import com.drakeet.multitype.ItemViewBinder

@SuppressWarnings("WeakerAccess")
class ContributorViewBinder(private var activity: AbsAboutActivityProxy) : ItemViewBinder<Contributor, ContributorViewBinder.ViewHolder>(){


    class ViewHolder(itemView: View, protected var activity: AbsAboutActivityProxy) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        private val avatar: ImageView = itemView.findViewById(R.id.avatar)
        private val name: TextView = itemView.findViewById(R.id.name)
        private val desc: TextView = itemView.findViewById(R.id.desc)
        private lateinit var data: Contributor


        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val listener = activity.onContributorClickedListener
            if (listener != null && listener.onContributorClicked(v!!, data)) {
                return
            }
            if (data.url != null) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = parse(data.url)

                try {
                    v.context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
        }

        fun bind(item: Contributor) {
            avatar.setImageResource(item.avatarResId)
            name.text = item.name
            desc.text = item.desc
            data = item
        }
    }

    override fun getItemId(item: Contributor): Long {
        return item.hashCode().toLong()
    }

    override fun onBindViewHolder(holder: ViewHolder, item: Contributor) {
        holder.bind(item)
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.about_page_item_contributor, parent, false), activity)
    }
}
