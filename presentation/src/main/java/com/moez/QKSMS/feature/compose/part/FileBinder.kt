/*
 * Copyright (C) 2019 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.moez.QKSMS.feature.compose.part

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import com.fulldive.extension.divesms.R
import com.moez.QKSMS.common.base.QkViewHolder
import com.moez.QKSMS.common.util.Colors
import com.moez.QKSMS.common.util.extensions.resolveThemeColor
import com.moez.QKSMS.common.util.extensions.setBackgroundTint
import com.moez.QKSMS.common.util.extensions.setTint
import com.moez.QKSMS.feature.compose.BubbleUtils
import com.moez.QKSMS.model.Message
import com.moez.QKSMS.model.MmsPart
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import com.fulldive.extension.divesms.databinding.MmsFileListItemBinding
import javax.inject.Inject

class FileBinder @Inject constructor(colors: Colors, private val context: Context) : PartBinder() {

    override val partLayout = R.layout.mms_file_list_item
    override var theme = colors.theme()

    // This is the last binder we check. If we're here, we can bind the part
    override fun canBindPart(part: MmsPart) = true

    @SuppressLint("CheckResult")
    override fun bindPart(
        holder: QkViewHolder,
        part: MmsPart,
        message: Message,
        canGroupWithPrevious: Boolean,
        canGroupWithNext: Boolean
    ) {
        val binding = MmsFileListItemBinding.bind(holder.itemView)
        
        BubbleUtils.getBubble(false, canGroupWithPrevious, canGroupWithNext, message.isMe())
                .let(binding.fileBackground::setBackgroundResource)

        holder.containerView.setOnClickListener { clicks.onNext(part.id) }

        Observable.just(part.getUri())
                .map(context.contentResolver::openInputStream)
                .map { inputStream -> inputStream.use { it.available() } }
                .map { bytes ->
                    when (bytes) {
                        in 0..999 -> "$bytes B"
                        in 1000..999999 -> "${"%.1f".format(bytes / 1000f)} KB"
                        in 1000000..9999999 -> "${"%.1f".format(bytes / 1000000f)} MB"
                        else -> "${"%.1f".format(bytes / 1000000000f)} GB"
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { size -> binding.size.text = size }

        binding.filename.text = part.name

        val params = binding.fileBackground.layoutParams as FrameLayout.LayoutParams
        if (!message.isMe()) {
            binding.fileBackground.layoutParams = params.apply { gravity = Gravity.START }
            binding.fileBackground.setBackgroundTint(theme.theme)
            binding.icon.setTint(theme.textPrimary)
            binding.filename.setTextColor(theme.textPrimary)
            binding.size.setTextColor(theme.textTertiary)
        } else {
            binding.fileBackground.layoutParams = params.apply { gravity = Gravity.END }
            binding.fileBackground.setBackgroundTint(holder.containerView.context.resolveThemeColor(R.attr.bubbleColor))
            binding.icon.setTint(holder.containerView.context.resolveThemeColor(android.R.attr.textColorSecondary))
            binding.filename.setTextColor(holder.containerView.context.resolveThemeColor(android.R.attr.textColorPrimary))
            binding.size.setTextColor(holder.containerView.context.resolveThemeColor(android.R.attr.textColorTertiary))
        }
    }

}