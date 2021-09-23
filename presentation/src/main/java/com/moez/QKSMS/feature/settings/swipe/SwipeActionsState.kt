
package com.moez.QKSMS.feature.settings.swipe

import androidx.annotation.DrawableRes
import com.moez.QKSMS.R

data class SwipeActionsState(
    @DrawableRes val rightIcon: Int = R.drawable.ic_archive_black_24dp,
    val rightLabel: String = "",

    @DrawableRes val leftIcon: Int = R.drawable.ic_archive_black_24dp,
    val leftLabel: String = ""
)