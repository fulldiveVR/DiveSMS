
package com.moez.QKSMS.compat

import android.content.Context
import android.database.sqlite.SqliteWrapper
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.Telephony
import android.text.TextUtils
import android.util.Patterns
import timber.log.Timber
import java.util.regex.Pattern

object TelephonyCompat {

    private val ID_PROJECTION = arrayOf(BaseColumns._ID)

    private val THREAD_ID_CONTENT_URI = Uri.parse("content://mms-sms/threadID")

    val NAME_ADDR_EMAIL_PATTERN = Pattern.compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*")

    fun getOrCreateThreadId(context: Context, recipient: String): Long {
        return getOrCreateThreadId(context, listOf(recipient))
    }

    fun getOrCreateThreadId(context: Context, recipients: Collection<String>): Long {
        return if (Build.VERSION.SDK_INT >= 23) {
            Telephony.Threads.getOrCreateThreadId(context, recipients.toSet())
        } else {
            val uriBuilder = THREAD_ID_CONTENT_URI.buildUpon()

            recipients
                    .map { recipient -> if (isEmailAddress(recipient)) extractAddrSpec(recipient) else recipient }
                    .forEach { recipient -> uriBuilder.appendQueryParameter("recipient", recipient) }

            val uri = uriBuilder.build()

            SqliteWrapper.query(context, context.contentResolver, uri, ID_PROJECTION, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0)
                } else {
                    Timber.e("getOrCreateThreadId returned no rows!")
                }
            }

            Timber.e("getOrCreateThreadId failed with " + recipients.size + " recipients")
            throw IllegalArgumentException("Unable to find or allocate a thread ID.")
        }
    }

    fun extractAddrSpec(address: String): String {
        val match = NAME_ADDR_EMAIL_PATTERN.matcher(address)
        return if (match.matches()) {
            match.group(2)
        } else address
    }

    fun isEmailAddress(address: String): Boolean {
        if (TextUtils.isEmpty(address)) {
            return false
        }
        val s = extractAddrSpec(address)
        val match = Patterns.EMAIL_ADDRESS.matcher(s)
        return match.matches()
    }

}