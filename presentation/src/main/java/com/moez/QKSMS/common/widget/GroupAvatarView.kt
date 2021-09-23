
package com.moez.QKSMS.common.widget

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.moez.QKSMS.R
import com.moez.QKSMS.model.Recipient
import kotlinx.android.synthetic.main.group_avatar_view.view.*

class GroupAvatarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

    var contacts: List<Recipient> = ArrayList()
        set(value) {
            field = value
            updateView()
        }

    private val avatars by lazy { listOf(avatar1, avatar2, avatar3) }

    init {
        View.inflate(context, R.layout.group_avatar_view, this)
        setBackgroundResource(R.drawable.circle)
        clipToOutline = true
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        avatars.forEach { avatar ->
            avatar.setBackgroundResource(R.drawable.rectangle)

            // If we're on API 21 we need to reapply the tint after changing the background
            if (Build.VERSION.SDK_INT < 22) {
                avatar.applyTheme(0)
            }
        }

        if (!isInEditMode) {
            updateView()
        }
    }

    private fun updateView() {
        avatars.forEachIndexed { index, avatar ->
            avatar.visibility = if (contacts.size > index) View.VISIBLE else View.GONE
            avatar.setContact(contacts.getOrNull(index))
        }
    }

}