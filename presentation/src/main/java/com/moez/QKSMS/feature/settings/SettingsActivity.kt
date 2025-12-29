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


package com.moez.QKSMS.feature.settings

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isGone
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.fulldive.extension.divesms.R
import com.moez.QKSMS.common.base.QkThemedActivity
import com.moez.QKSMS.email.GmailOAuthManager
import dagger.android.AndroidInjection
import com.fulldive.extension.divesms.databinding.ContainerActivityBinding
import timber.log.Timber
import javax.inject.Inject

class SettingsActivity : QkThemedActivity() {

    @Inject lateinit var gmailOAuthManager: GmailOAuthManager

    private lateinit var router: Router
    private lateinit var binding: ContainerActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        binding = ContainerActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.isGone = true

        router = Conductor.attachRouter(this, binding.container, savedInstanceState)
        if (!router.hasRootController()) {
            router.setRoot(RouterTransaction.with(SettingsController()))
        }
    }

    override fun onBackPressed() {
        if (!router.handleBack()) {
            super.onBackPressed()
        }
    }

    fun startGmailSignIn() {
        val signInIntent = gmailOAuthManager.getSignInIntent()
        startActivityForResult(signInIntent, GmailOAuthManager.RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.i("SettingsActivity: onActivityResult requestCode=$requestCode, resultCode=$resultCode")

        if (requestCode == GmailOAuthManager.RC_SIGN_IN) {
            Timber.i("SettingsActivity: Processing Gmail sign-in result")
            val email = gmailOAuthManager.handleSignInResultAndGetEmail(data)
            Timber.i("SettingsActivity: Gmail sign-in email=$email")
            // Forward result to the SettingsController
            val controller = router.backstack.lastOrNull()?.controller as? SettingsController
            Timber.i("SettingsActivity: Controller found=${controller != null}")
            controller?.onGmailSignInResult(email)
        }
    }

}