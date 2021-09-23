
package com.moez.QKSMS.manager

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.moez.QKSMS.receiver.SendScheduledMessageReceiver
import javax.inject.Inject

class AlarmManagerImpl @Inject constructor(private val context: Context) : AlarmManager {

    override fun getScheduledMessageIntent(id: Long): PendingIntent {
        val intent = Intent(context, SendScheduledMessageReceiver::class.java).putExtra("id", id)
        return PendingIntent.getBroadcast(context, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun setAlarm(date: Long, intent: PendingIntent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, date, intent)
        } else {
            alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, date, intent)
        }
    }

}