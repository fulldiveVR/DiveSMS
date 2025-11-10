/*
 *  Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 *  This file is part of QKSMS.
 *
 *  QKSMS is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  QKSMS is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 *
 */


package com.moez.QKSMS.feature.changelog

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import com.fulldive.extension.divesms.R
import com.moez.QKSMS.common.base.QkAdapter
import com.moez.QKSMS.common.base.QkViewHolder
import com.moez.QKSMS.manager.ChangelogManager
import com.fulldive.extension.divesms.databinding.ChangelogListItemBinding

class ChangelogAdapter(private val context: Context) : QkAdapter<ChangelogAdapter.ChangelogItem>() {

    data class ChangelogItem(val type: Int, val label: String)

    fun setChangelog(changelog: ChangelogManager.CumulativeChangelog) {
        val changes = mutableListOf<ChangelogItem>()
        if (changelog.added.isNotEmpty()) {
            changes += ChangelogItem(0, context.getString(R.string.changelog_added))
            changes += changelog.added.map { change -> ChangelogItem(1, "• $change") }
            changes += ChangelogItem(0, "")
        }
        if (changelog.improved.isNotEmpty()) {
            changes += ChangelogItem(0, context.getString(R.string.changelog_improved))
            changes += changelog.improved.map { change -> ChangelogItem(1, "• $change") }
            changes += ChangelogItem(0, "")
        }
        if (changelog.fixed.isNotEmpty()) {
            changes += ChangelogItem(0, context.getString(R.string.changelog_fixed))
            changes += changelog.fixed.map { change -> ChangelogItem(1, "• $change") }
            changes += ChangelogItem(0, "")
        }
        data = changes
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val binding = ChangelogListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QkViewHolder(binding.root).apply {
            if (viewType == 0) {
                binding.changelogItem.setTypeface(binding.changelogItem.typeface, Typeface.BOLD)
            }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val item = getItem(position)
        val binding = ChangelogListItemBinding.bind(holder.itemView)

        binding.changelogItem.text = item.label
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

}
