
package com.moez.QKSMS.feature.changelog

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import com.moez.QKSMS.R
import com.moez.QKSMS.common.base.QkAdapter
import com.moez.QKSMS.common.base.QkViewHolder
import com.moez.QKSMS.manager.ChangelogManager
import kotlinx.android.synthetic.main.changelog_list_item.*

class ChangelogAdapter(private val context: Context) : QkAdapter<ChangelogAdapter.ChangelogItem>() {

    data class ChangelogItem(val type: Int, val label: String)

    fun setChangelog(changelog: ChangelogManager.Changelog) {
        val changes = mutableListOf<ChangelogItem>()
        if (changelog.added.isNotEmpty()) {
            changes += ChangelogItem(0, context.getString(R.string.changelog_added))
            changes += changelog.added.map { change -> ChangelogItem(1, change) }
            changes += ChangelogItem(0, "")
        }
        if (changelog.improved.isNotEmpty()) {
            changes += ChangelogItem(0, context.getString(R.string.changelog_improved))
            changes += changelog.improved.map { change -> ChangelogItem(1, change) }
            changes += ChangelogItem(0, "")
        }
        if (changelog.fixed.isNotEmpty()) {
            changes += ChangelogItem(0, context.getString(R.string.changelog_fixed))
            changes += changelog.fixed.map { change -> ChangelogItem(1, change) }
            changes += ChangelogItem(0, "")
        }
        data = changes
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.changelog_list_item, parent, false)
        return QkViewHolder(view).apply {
            if (viewType == 0) {
                changelogItem.setTypeface(changelogItem.typeface, Typeface.BOLD)
            }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val item = getItem(position)

        holder.changelogItem.text = item.label
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

}
