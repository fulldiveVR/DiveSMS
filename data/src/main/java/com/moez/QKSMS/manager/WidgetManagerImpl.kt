
package com.moez.QKSMS.manager

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.klinker.android.send_message.BroadcastUtils
import javax.inject.Inject

class WidgetManagerImpl @Inject constructor(private val context: Context) : WidgetManager {

    override fun updateUnreadCount() {
        BroadcastUtils.sendExplicitBroadcast(context, Intent(), WidgetManager.ACTION_NOTIFY_DATASET_CHANGED)
    }

    override fun updateTheme() {
        val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName("com.moez.QKSMS", "com.moez.QKSMS.feature.widget.WidgetProvider"))

        val intent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)

        BroadcastUtils.sendExplicitBroadcast(context, intent, AppWidgetManager.ACTION_APPWIDGET_UPDATE)
    }

}