/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.moez.QKSMS.feature.forwarding

import android.content.Context
import com.moez.QKSMS.common.base.QkViewModel
import com.moez.QKSMS.common.util.ForwardingNotificationManager
import com.moez.QKSMS.common.util.extensions.makeToast
import com.moez.QKSMS.email.EmailService
import com.moez.QKSMS.manager.ForwardingStatusManager
import com.moez.QKSMS.model.ForwardingStatus
import com.moez.QKSMS.model.ForwardingType
import com.moez.QKSMS.repository.ForwardingRepository
import com.moez.QKSMS.telegram.TelegramService
import com.moez.QKSMS.util.Preferences
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class ForwardingViewModel @Inject constructor(
    private val context: Context,
    private val forwardingRepo: ForwardingRepository,
    private val prefs: Preferences,
    private val telegramService: TelegramService,
    private val emailService: EmailService,
    private val statusManager: ForwardingStatusManager,
    private val notificationManager: ForwardingNotificationManager
) : QkViewModel<ForwardingView, ForwardingState>(ForwardingState()) {

    init {
        disposables += prefs.emailForwardingEnabled.asObservable()
            .subscribe { enabled ->
                newState { copy(forwardingEnabled = enabled) }
                updateEmailStatus()
            }

        disposables += prefs.emailForwardingAddress.asObservable()
            .subscribe { email -> newState { copy(forwardingEmail = email) } }

        // Telegram
        disposables += prefs.telegramForwardingEnabled.asObservable()
            .subscribe { enabled ->
                newState { copy(telegramEnabled = enabled) }
                updateTelegramStatus()
            }

        disposables += prefs.telegramChatId.asObservable()
            .subscribe { chatId -> newState { copy(telegramChatId = chatId) } }

        // Status subscriptions
        disposables += statusManager.emailStatus
            .subscribe { status -> newState { copy(emailStatus = status) } }

        disposables += statusManager.telegramStatus
            .subscribe { status -> newState { copy(telegramStatus = status) } }

        updateForwardingAccountState()
        updateEmailStatus()
        updateTelegramStatus()
    }

    private fun updateEmailStatus() {
        val status = if (prefs.emailForwardingEnabled.get()) {
            statusManager.getEmailStatus()
        } else {
            ForwardingStatus.Disabled
        }
        newState { copy(emailStatus = status) }
    }

    private fun updateTelegramStatus() {
        val status = if (prefs.telegramForwardingEnabled.get()) {
            statusManager.getTelegramStatus()
        } else {
            ForwardingStatus.Disabled
        }
        newState { copy(telegramStatus = status) }
    }

    private fun updateForwardingAccountState() {
        val account = forwardingRepo.getDefaultEmailAccount()
        val accountName = account?.email ?: ""
        newState { copy(forwardingAccountName = accountName) }
    }

    override fun bindView(view: ForwardingView) {
        super.bindView(view)

        view.forwardingEnabledIntent
            .autoDispose(view.scope())
            .subscribe { prefs.emailForwardingEnabled.set(!prefs.emailForwardingEnabled.get()) }

        view.forwardingEmailIntent
            .autoDispose(view.scope())
            .subscribe { view.showForwardingEmailDialog(prefs.emailForwardingAddress.get()) }

        view.forwardingEmailChangedIntent
            .doOnNext(prefs.emailForwardingAddress::set)
            .autoDispose(view.scope())
            .subscribe()

        view.forwardingAccountIntent
            .autoDispose(view.scope())
            .subscribe {
                val account = forwardingRepo.getDefaultEmailAccount()
                if (account != null && account.email.isNotBlank()) {
                    view.showGmailSignOutDialog()
                } else {
                    view.requestGmailSignIn()
                }
            }

        // Telegram bindings
        view.telegramEnabledIntent
            .autoDispose(view.scope())
            .subscribe { prefs.telegramForwardingEnabled.set(!prefs.telegramForwardingEnabled.get()) }

        view.telegramChatIdIntent
            .autoDispose(view.scope())
            .subscribe { view.showTelegramChatIdDialog(prefs.telegramChatId.get()) }

        view.telegramChatIdChangedIntent
            .doOnNext(prefs.telegramChatId::set)
            .autoDispose(view.scope())
            .subscribe()

        view.emailTestIntent
            .autoDispose(view.scope())
            .subscribe { sendTestEmail() }

        view.telegramTestIntent
            .autoDispose(view.scope())
            .subscribe { sendTestTelegramMessage(view) }

        view.telegramHelpIntent
            .autoDispose(view.scope())
            .subscribe { view.openTelegramBot() }

        // Status action handlers
        view.emailStatusActionIntent
            .autoDispose(view.scope())
            .subscribe { handleEmailStatusAction(view) }

        view.telegramStatusActionIntent
            .autoDispose(view.scope())
            .subscribe { handleTelegramStatusAction() }
    }

    private fun handleEmailStatusAction(view: ForwardingView) {
        val status = statusManager.getEmailStatus()
        when (status) {
            is ForwardingStatus.NeedsReconnect, is ForwardingStatus.Disconnected -> {
                // User needs to sign in again
                view.requestGmailSignIn()
            }
            is ForwardingStatus.Pending -> {
                // Clear failures and dismiss notification
                statusManager.clearFailures(ForwardingType.EMAIL)
                notificationManager.dismissNotification(ForwardingType.EMAIL)
                context.makeToast(com.fulldive.extension.divesms.R.string.forwarding_status_cleared)
            }
            else -> {}
        }
    }

    private fun handleTelegramStatusAction() {
        // Clear failures and dismiss notification
        statusManager.clearFailures(ForwardingType.TELEGRAM)
        notificationManager.dismissNotification(ForwardingType.TELEGRAM)
        context.makeToast(com.fulldive.extension.divesms.R.string.forwarding_status_cleared)
    }

    private fun sendTestEmail() {
        val destinationEmail = prefs.emailForwardingAddress.get()
        if (destinationEmail.isBlank()) {
            context.makeToast(com.fulldive.extension.divesms.R.string.forwarding_email_no_destination)
            return
        }

        val emailAccount = forwardingRepo.getDefaultEmailAccount()
        if (emailAccount == null) {
            context.makeToast(com.fulldive.extension.divesms.R.string.forwarding_email_no_account)
            return
        }

        if (!emailService.isAccountReady(emailAccount)) {
            context.makeToast(com.fulldive.extension.divesms.R.string.forwarding_email_account_not_ready)
            return
        }

        // Copy Realm object values to avoid thread access issues
        val accountCopy = com.moez.QKSMS.model.EmailAccount(
            id = emailAccount.id,
            accountType = emailAccount.accountType,
            email = emailAccount.email,
            gmailAccountName = emailAccount.gmailAccountName,
            smtpHost = emailAccount.smtpHost,
            smtpPort = emailAccount.smtpPort,
            smtpUsername = emailAccount.smtpUsername,
            smtpUseTls = emailAccount.smtpUseTls,
            isDefault = emailAccount.isDefault,
            createdAt = emailAccount.createdAt,
            lastUsed = emailAccount.lastUsed
        )

        CoroutineScope(Dispatchers.IO).launch {
            val result = emailService.sendEmail(
                to = destinationEmail,
                subject = "Wize SMS Test Message",
                body = "This is a test message from Wize SMS.\n\nIf you received this email, your email forwarding is working correctly!",
                isHtml = false,
                account = accountCopy
            )

            withContext(Dispatchers.Main) {
                when (result) {
                    is EmailService.SendResult.Success -> {
                        context.makeToast(com.fulldive.extension.divesms.R.string.forwarding_email_test_success)
                    }
                    is EmailService.SendResult.Failure -> {
                        Timber.e("Failed to send test email: ${result.error}")
                        context.makeToast(com.fulldive.extension.divesms.R.string.forwarding_email_test_failed)
                    }
                    is EmailService.SendResult.AuthRequired -> {
                        Timber.w("Auth required for test email: ${result.message}")
                        context.makeToast(com.fulldive.extension.divesms.R.string.forwarding_email_account_not_ready)
                    }
                }
            }
        }
    }

    private fun sendTestTelegramMessage(view: ForwardingView) {
        val chatId = prefs.telegramChatId.get()
        if (chatId.isBlank()) {
            context.makeToast(com.fulldive.extension.divesms.R.string.forwarding_telegram_no_chat_id)
            return
        }

        disposables += telegramService.sendTestMessage(chatId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { success ->
                    if (success) {
                        context.makeToast(com.fulldive.extension.divesms.R.string.forwarding_telegram_test_success)
                    } else {
                        context.makeToast(com.fulldive.extension.divesms.R.string.forwarding_telegram_test_failed)
                    }
                },
                { error ->
                    Timber.e(error, "Failed to send test Telegram message")
                    context.makeToast(com.fulldive.extension.divesms.R.string.forwarding_telegram_test_failed)
                }
            )
    }

    fun onGmailSignInResult(email: String?) {
        Timber.i("ForwardingViewModel: onGmailSignInResult called with email=$email")
        if (email != null) {
            Timber.i("ForwardingViewModel: Saving Gmail account to Realm...")
            val accountId = forwardingRepo.saveEmailAccount(
                accountType = "GMAIL",
                email = email,
                gmailAccountName = email,
                isDefault = true
            )
            Timber.i("ForwardingViewModel: saveEmailAccount returned id=$accountId")
            updateForwardingAccountState()
        } else {
            Timber.w("ForwardingViewModel: Gmail sign-in returned null email")
        }
    }

    fun onGmailSignOut() {
        val account = forwardingRepo.getDefaultEmailAccount()
        if (account != null) {
            forwardingRepo.deleteEmailAccount(account.id)
        }
        updateForwardingAccountState()
    }
}
