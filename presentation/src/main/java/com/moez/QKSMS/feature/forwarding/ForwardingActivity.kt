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
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
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

    private val forwardingEmailDialog: TextInputDialog by lazy {
        TextInputDialog(this, getString(R.string.settings_forwarding_email_title), forwardingEmailSubject::onNext)
    }

    override val forwardingEnabledIntent by lazy { binding.forwardingEnabled.clicks() }
    override val forwardingEmailIntent by lazy { binding.forwardingEmail.clicks() }
    override val forwardingEmailChangedIntent: Observable<String> = forwardingEmailSubject
    override val forwardingAccountIntent by lazy { binding.forwardingAccount.clicks() }

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
}
