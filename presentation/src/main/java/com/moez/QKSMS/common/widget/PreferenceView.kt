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


package com.moez.QKSMS.common.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import com.fulldive.extension.divesms.R
import com.moez.QKSMS.common.util.extensions.resolveThemeAttribute
import com.moez.QKSMS.common.util.extensions.resolveThemeColorStateList
import com.moez.QKSMS.common.util.extensions.setVisible
import com.moez.QKSMS.injection.appComponent
import com.fulldive.extension.divesms.databinding.PreferenceViewBinding

class PreferenceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayoutCompat(context, attrs) {

    lateinit var binding: PreferenceViewBinding

    var title: String? = null
        set(value) {
            field = value

            if (isInEditMode) {
                findViewById<TextView>(R.id.titleView).text = value
            } else {
                binding.titleView.text = value
            }
        }

    var summary: String? = null
        set(value) {
            field = value

            if (isInEditMode) {
                findViewById<TextView>(R.id.summaryView).run {
                    text = value
                    setVisible(value?.isNotEmpty() == true)
                }
            } else {
                binding.summaryView.text = value
                binding.summaryView.setVisible(value?.isNotEmpty() == true)
            }
        }

    init {
        if (!isInEditMode) {
            appComponent.inject(this)
        }

        binding = PreferenceViewBinding.inflate(LayoutInflater.from(context), this)
        setBackgroundResource(context.resolveThemeAttribute(android.R.attr.selectableItemBackground))
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        binding.icon.imageTintList = context.resolveThemeColorStateList(android.R.attr.textColorSecondary)

        context.obtainStyledAttributes(attrs, R.styleable.PreferenceView).run {
            title = getString(R.styleable.PreferenceView_title)
            summary = getString(R.styleable.PreferenceView_summary)

            // If there's a custom view used for the preference's widget, inflate it
            getResourceId(R.styleable.PreferenceView_widget, -1).takeIf { it != -1 }?.let { id ->
                View.inflate(context, id, binding.widgetFrame)
            }

            // If an icon is being used, set up the icon view
            getResourceId(R.styleable.PreferenceView_icon, -1).takeIf { it != -1 }?.let { id ->
                binding.icon.setVisible(true)
                binding.icon.setImageResource(id)
            }

            recycle()
        }
    }

}