
package com.moez.QKSMS.feature.conversationinfo

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.moez.QKSMS.R
import com.moez.QKSMS.common.Navigator
import com.moez.QKSMS.common.base.QkRealmAdapter
import com.moez.QKSMS.common.base.QkViewHolder
import com.moez.QKSMS.common.util.extensions.setVisible
import com.moez.QKSMS.extensions.isVideo
import com.moez.QKSMS.model.MmsPart
import com.moez.QKSMS.util.GlideApp
import kotlinx.android.synthetic.main.conversation_media_list_item.view.*
import javax.inject.Inject

class ConversationMediaAdapter @Inject constructor(
    private val context: Context,
    private val navigator: Navigator
) : QkRealmAdapter<MmsPart>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.conversation_media_list_item, parent, false)
        return QkViewHolder(view).apply {
            view.thumbnail.setOnClickListener {
                val part = getItem(adapterPosition) ?: return@setOnClickListener
                navigator.showMedia(part.id)
            }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val part = getItem(position) ?: return
        val view = holder.containerView

        GlideApp.with(context)
                .load(part.getUri())
                .fitCenter()
                .into(view.thumbnail)

        view.video.setVisible(part.isVideo())
    }

}