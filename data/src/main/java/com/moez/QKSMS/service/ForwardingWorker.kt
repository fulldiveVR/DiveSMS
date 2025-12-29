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

package com.moez.QKSMS.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Background worker for retrying failed email forwards.
 *
 * The initial email send happens synchronously via ForwardSms interactor.
 * This worker handles:
 * - Retrying failed email sends
 * - Processing queued forwards when network becomes available
 */
class ForwardingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val logId = inputData.getLong(KEY_LOG_ID, -1L)

        if (logId == -1L) {
            Timber.e("ForwardingWorker: No log ID provided")
            return Result.failure()
        }

        Timber.d("ForwardingWorker: Retrying email for log $logId (attempt ${runAttemptCount + 1})")

        return try {
            // Get dependencies through ForwardingWorkerHelper singleton
            // This is initialized by the Application
            val helper = ForwardingWorkerHelper.instance
            if (helper == null) {
                Timber.e("ForwardingWorker: Helper not initialized")
                return Result.failure()
            }

            val result = helper.retryEmailSend(logId)

            when {
                result.success -> {
                    Timber.d("ForwardingWorker: Email retry successful for log $logId")
                    Result.success()
                }
                result.shouldRetry && runAttemptCount < MAX_RETRIES -> {
                    Timber.d("ForwardingWorker: Scheduling retry ${runAttemptCount + 1}/$MAX_RETRIES")
                    Result.retry()
                }
                else -> {
                    Timber.e("ForwardingWorker: Email retry failed permanently for log $logId")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "ForwardingWorker: Failed to process retry for log $logId")

            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val KEY_LOG_ID = "logId"
        private const val MAX_RETRIES = 3
        private const val TAG_RETRY = "email_forwarding_retry"

        /**
         * Retry a specific failed forwarding log entry.
         */
        fun retryLog(context: Context, logId: Long) {
            Timber.d("ForwardingWorker: Scheduling retry for log $logId")

            val data = workDataOf(KEY_LOG_ID to logId)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ForwardingWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .addTag(TAG_RETRY)
                .addTag("log_$logId")
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}

/**
 * Helper class for ForwardingWorker to access dependencies.
 * Must be initialized by the Application during startup.
 */
object ForwardingWorkerHelper {
    var instance: ForwardingWorkerHelperImpl? = null
        private set

    fun init(helper: ForwardingWorkerHelperImpl) {
        instance = helper
        Timber.d("ForwardingWorkerHelper: Initialized")
    }
}

/**
 * Interface for the worker helper implementation.
 * Implemented in the data module and initialized from the app module.
 */
interface ForwardingWorkerHelperImpl {
    data class RetryResult(val success: Boolean, val shouldRetry: Boolean)

    suspend fun retryEmailSend(logId: Long): RetryResult
}
