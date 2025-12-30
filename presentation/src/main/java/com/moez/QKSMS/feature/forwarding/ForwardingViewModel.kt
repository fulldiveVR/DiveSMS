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
import com.moez.QKSMS.common.util.extensions.makeToast
import com.moez.QKSMS.repository.ForwardingRepository
import com.moez.QKSMS.telegram.TelegramService
import com.moez.QKSMS.util.Preferences
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
    private val telegramService: TelegramService
) : QkViewModel<ForwardingView, ForwardingState>(ForwardingState()) {

    init {
        disposables += prefs.emailForwardingEnabled.asObservable()
            .subscribe { enabled -> newState { copy(forwardingEnabled = enabled) } }

        disposables += prefs.emailForwardingAddress.asObservable()
            .subscribe { email -> newState { copy(forwardingEmail = email) } }

        // Telegram
        disposables += prefs.telegramForwardingEnabled.asObservable()
            .subscribe { enabled -> newState { copy(telegramEnabled = enabled) } }

        disposables += prefs.telegramChatId.asObservable()
            .subscribe { chatId -> newState { copy(telegramChatId = chatId) } }

        updateForwardingAccountState()
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

        view.telegramTestIntent
            .autoDispose(view.scope())
            .subscribe { sendTestTelegramMessage(view) }

        view.telegramHelpIntent
            .autoDispose(view.scope())
            .subscribe { view.openTelegramBot() }
    }

    private fun sendTestTelegramMessage(view: ForwardingView) {
        val chatId = prefs.telegramChatId.get()
        if (chatId.isBlank()) {
            context.makeToast(com.fulldive.extension.divesms.R.string.forwarding_telegram_no_chat_id)
            return
        }

        disposables += telegramService.sendTestMessage(chatId)
            .subscribeOn(Schedulers.io())
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
