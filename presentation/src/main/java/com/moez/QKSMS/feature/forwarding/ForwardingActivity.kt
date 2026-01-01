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
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.jakewharton.rxbinding2.view.clicks
import com.fulldive.extension.divesms.R
import com.fulldive.extension.divesms.databinding.ForwardingActivityBinding
import com.moez.QKSMS.common.base.QkThemedActivity
import com.moez.QKSMS.common.util.FontProvider
import com.moez.QKSMS.common.util.extensions.makeToast
import com.moez.QKSMS.common.widget.QkSwitch
import com.moez.QKSMS.common.widget.TextInputDialog
import com.moez.QKSMS.email.GmailOAuthManager
import com.moez.QKSMS.model.ForwardingStatus
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import javax.inject.Inject

class ForwardingActivity : QkThemedActivity(), ForwardingView {

    @Inject lateinit var context: Context
    @Inject lateinit var fontProvider: FontProvider
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var gmailOAuthManager: GmailOAuthManager

    private lateinit var binding: ForwardingActivityBinding

    private val forwardingEmailSubject: Subject<String> = PublishSubject.create()
    private val telegramChatIdSubject: Subject<String> = PublishSubject.create()

    private val forwardingEmailDialog: TextInputDialog by lazy {
        TextInputDialog(this, getString(R.string.settings_forwarding_email_title), forwardingEmailSubject::onNext)
    }

    private val telegramChatIdDialog: TextInputDialog by lazy {
        TextInputDialog(this, getString(R.string.forwarding_telegram_chat_id_dialog_title), telegramChatIdSubject::onNext)
    }

    override val forwardingEnabledIntent by lazy { binding.forwardingEnabled.clicks() }
    override val forwardingEmailIntent by lazy { binding.forwardingEmail.clicks() }
    override val forwardingEmailChangedIntent: Observable<String> = forwardingEmailSubject
    override val forwardingAccountIntent by lazy { binding.forwardingAccount.clicks() }
    override val emailTestIntent by lazy { binding.emailTest.clicks() }
    override val emailStatusActionIntent by lazy {
        Observable.merge(
            binding.emailStatusBanner.clicks(),
            binding.emailStatusAction.clicks()
        )
    }

    // Telegram
    override val telegramEnabledIntent by lazy { binding.telegramEnabled.clicks() }
    override val telegramChatIdIntent by lazy { binding.telegramChatId.clicks() }
    override val telegramChatIdChangedIntent: Observable<String> = telegramChatIdSubject
    override val telegramTestIntent by lazy { binding.telegramTest.clicks() }
    override val telegramHelpIntent by lazy { binding.telegramHelp.clicks() }
    override val telegramStatusActionIntent by lazy {
        Observable.merge(
            binding.telegramStatusBanner.clicks(),
            binding.telegramStatusAction.clicks()
        )
    }

    private val viewModel by lazy { ViewModelProviders.of(this, viewModelFactory)[ForwardingViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        binding = ForwardingActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTitle(R.string.forwarding_title)
        showBackButton(true)
        viewModel.bindView(this)

        if (!prefs.systemFont.get()) {
            fontProvider.getLato { lato ->
                val typeface = Typeface.create(lato, Typeface.BOLD)
                binding.collapsingToolbar.collapsingToolbar.setCollapsedTitleTypeface(typeface)
                binding.collapsingToolbar.collapsingToolbar.setExpandedTitleTypeface(typeface)
            }
        }
    }

    override fun render(state: ForwardingState) {
        binding.forwardingEnabled.findViewById<QkSwitch>(R.id.checkbox).isChecked = state.forwardingEnabled
        binding.forwardingEmail.summary = state.forwardingEmail.takeIf { it.isNotBlank() }
            ?: getString(R.string.settings_forwarding_email_summary)
        binding.forwardingAccount.summary = when {
            state.forwardingAccountName.isNotBlank() -> getString(R.string.settings_forwarding_account_summary, state.forwardingAccountName)
            else -> getString(R.string.settings_forwarding_account_summary_none)
        }

        // Email status
        renderEmailStatus(state.emailStatus, state.forwardingEnabled)

        // Telegram
        binding.telegramEnabled.findViewById<QkSwitch>(R.id.checkbox).isChecked = state.telegramEnabled
        binding.telegramChatId.summary = state.telegramChatId.takeIf { it.isNotBlank() }
            ?.let { getString(R.string.forwarding_telegram_chat_id_summary_set, it) }
            ?: getString(R.string.forwarding_telegram_chat_id_summary)

        // Telegram status
        renderTelegramStatus(state.telegramStatus, state.telegramEnabled)
    }

    private fun renderEmailStatus(status: ForwardingStatus, isEnabled: Boolean) {
        // Status chip
        when {
            !isEnabled -> {
                binding.emailStatus.visibility = View.GONE
                binding.emailStatusBanner.visibility = View.GONE
            }
            status is ForwardingStatus.Connected -> {
                binding.emailStatus.visibility = View.VISIBLE
                binding.emailStatus.text = getString(R.string.forwarding_status_connected)
                binding.emailStatus.setTextColor(ContextCompat.getColor(this, R.color.status_connected))
                binding.emailStatus.setBackgroundResource(R.drawable.rounded_rectangle_status_connected)
                binding.emailStatusBanner.visibility = View.GONE
            }
            status is ForwardingStatus.Pending -> {
                binding.emailStatus.visibility = View.VISIBLE
                binding.emailStatus.text = getString(R.string.forwarding_status_pending)
                binding.emailStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
                binding.emailStatus.setBackgroundResource(R.drawable.rounded_rectangle_status_warning)
                // Show banner
                binding.emailStatusBanner.visibility = View.VISIBLE
                binding.emailStatusTitle.text = getString(R.string.forwarding_status_pending_title)
                binding.emailStatusMessage.text = resources.getQuantityString(
                    R.plurals.forwarding_status_failed_messages,
                    status.failedCount,
                    status.failedCount
                )
                binding.emailStatusAction.text = getString(R.string.forwarding_status_action_retry)
            }
            status is ForwardingStatus.NeedsReconnect -> {
                binding.emailStatus.visibility = View.VISIBLE
                binding.emailStatus.text = getString(R.string.forwarding_status_needs_reconnect)
                binding.emailStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
                binding.emailStatus.setBackgroundResource(R.drawable.rounded_rectangle_status_warning)
                // Show banner
                binding.emailStatusBanner.visibility = View.VISIBLE
                binding.emailStatusTitle.text = getString(R.string.forwarding_status_reconnect_title)
                binding.emailStatusMessage.text = getString(R.string.forwarding_status_reconnect_message)
                binding.emailStatusAction.text = getString(R.string.forwarding_status_action_fix)
            }
            status is ForwardingStatus.Disconnected -> {
                binding.emailStatus.visibility = View.VISIBLE
                binding.emailStatus.text = getString(R.string.forwarding_status_disconnected)
                binding.emailStatus.setTextColor(ContextCompat.getColor(this, R.color.status_error))
                binding.emailStatus.setBackgroundResource(R.drawable.rounded_rectangle_status_error)
                // Show banner
                binding.emailStatusBanner.visibility = View.VISIBLE
                binding.emailStatusTitle.text = getString(R.string.forwarding_status_disconnected_title)
                binding.emailStatusMessage.text = getString(R.string.forwarding_status_disconnected_message)
                binding.emailStatusAction.text = getString(R.string.forwarding_status_action_fix)
            }
            else -> {
                binding.emailStatus.visibility = View.GONE
                binding.emailStatusBanner.visibility = View.GONE
            }
        }
    }

    private fun renderTelegramStatus(status: ForwardingStatus, isEnabled: Boolean) {
        when {
            !isEnabled -> {
                binding.telegramStatus.visibility = View.GONE
                binding.telegramStatusBanner.visibility = View.GONE
            }
            status is ForwardingStatus.Connected -> {
                binding.telegramStatus.visibility = View.VISIBLE
                binding.telegramStatus.text = getString(R.string.forwarding_status_connected)
                binding.telegramStatus.setTextColor(ContextCompat.getColor(this, R.color.status_connected))
                binding.telegramStatus.setBackgroundResource(R.drawable.rounded_rectangle_status_connected)
                binding.telegramStatusBanner.visibility = View.GONE
            }
            status is ForwardingStatus.Pending -> {
                binding.telegramStatus.visibility = View.VISIBLE
                binding.telegramStatus.text = getString(R.string.forwarding_status_pending)
                binding.telegramStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
                binding.telegramStatus.setBackgroundResource(R.drawable.rounded_rectangle_status_warning)
                // Show banner
                binding.telegramStatusBanner.visibility = View.VISIBLE
                binding.telegramStatusTitle.text = getString(R.string.forwarding_status_pending_title)
                binding.telegramStatusMessage.text = resources.getQuantityString(
                    R.plurals.forwarding_status_failed_messages,
                    status.failedCount,
                    status.failedCount
                )
                binding.telegramStatusAction.text = getString(R.string.forwarding_status_action_retry)
            }
            status is ForwardingStatus.NeedsReconnect || status is ForwardingStatus.Disconnected -> {
                val failedCount = when (status) {
                    is ForwardingStatus.NeedsReconnect -> status.failedCount
                    is ForwardingStatus.Disconnected -> status.failedCount
                    else -> 0
                }
                binding.telegramStatus.visibility = View.VISIBLE
                binding.telegramStatus.text = getString(R.string.forwarding_status_issue)
                binding.telegramStatus.setTextColor(ContextCompat.getColor(this, R.color.status_error))
                binding.telegramStatus.setBackgroundResource(R.drawable.rounded_rectangle_status_error)
                // Show banner
                binding.telegramStatusBanner.visibility = View.VISIBLE
                binding.telegramStatusTitle.text = getString(R.string.forwarding_status_telegram_issue_title)
                binding.telegramStatusMessage.text = if (failedCount > 0) {
                    resources.getQuantityString(
                        R.plurals.forwarding_status_failed_messages,
                        failedCount,
                        failedCount
                    )
                } else {
                    getString(R.string.forwarding_status_telegram_issue_message)
                }
                binding.telegramStatusAction.text = getString(R.string.forwarding_status_action_retry)
            }
            else -> {
                binding.telegramStatus.visibility = View.GONE
                binding.telegramStatusBanner.visibility = View.GONE
            }
        }
    }

    override fun showForwardingEmailDialog(email: String) {
        forwardingEmailDialog.setText(email).show()
    }

    override fun requestGmailSignIn() {
        val signInIntent = gmailOAuthManager.getSignInIntent()
        startActivityForResult(signInIntent, GmailOAuthManager.RC_SIGN_IN)
    }

    override fun showGmailSignOutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_forwarding_account_title)
            .setMessage(R.string.settings_forwarding_signout_message)
            .setNegativeButton(R.string.button_cancel, null)
            .setPositiveButton(R.string.settings_forwarding_signout_button) { _, _ ->
                viewModel.onGmailSignOut()
                makeToast(R.string.settings_forwarding_signed_out)
            }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.i("ForwardingActivity: onActivityResult requestCode=$requestCode, resultCode=$resultCode")

        if (requestCode == GmailOAuthManager.RC_SIGN_IN) {
            Timber.i("ForwardingActivity: Processing Gmail sign-in result")
            val email = gmailOAuthManager.handleSignInResultAndGetEmail(data)
            Timber.i("ForwardingActivity: Gmail sign-in email=$email")
            viewModel.onGmailSignInResult(email)
            if (email != null) {
                makeToast(R.string.settings_forwarding_signed_in)
            }
        }
    }

    override fun showTelegramChatIdDialog(chatId: String) {
        telegramChatIdDialog.setText(chatId).show()
    }

    override fun openTelegramBot() {
        try {
            // Try to open in Telegram app
            val telegramIntent = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=WizeSMSForwardBot"))
            startActivity(telegramIntent)
        } catch (e: Exception) {
            // Fall back to web browser
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/WizeSMSForwardBot"))
            startActivity(webIntent)
        }
    }
}
