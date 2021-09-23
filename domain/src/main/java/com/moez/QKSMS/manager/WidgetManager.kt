
package com.moez.QKSMS.manager

interface WidgetManager {

    companion object {
        const val ACTION_NOTIFY_DATASET_CHANGED = "com.moez.QKSMS.intent.action.ACTION_NOTIFY_DATASET_CHANGED"
    }

    fun updateUnreadCount()

    fun updateTheme()

}