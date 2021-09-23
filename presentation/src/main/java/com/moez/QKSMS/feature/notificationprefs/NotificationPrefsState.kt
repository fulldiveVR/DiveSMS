
package com.moez.QKSMS.feature.notificationprefs

import android.os.Build
import com.moez.QKSMS.util.Preferences

data class NotificationPrefsState(
    val threadId: Long = 0,
    val conversationTitle: String = "",
    val notificationsEnabled: Boolean = true,
    val previewSummary: String = "",
    val previewId: Int = Preferences.NOTIFICATION_PREVIEWS_ALL,
    val action1Summary: String = "",
    val action2Summary: String = "",
    val action3Summary: String = "",
    val vibrationEnabled: Boolean = true,
    val ringtoneName: String = "",
    val qkReplyEnabled: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.N,
    val qkReplyTapDismiss: Boolean = true
)
