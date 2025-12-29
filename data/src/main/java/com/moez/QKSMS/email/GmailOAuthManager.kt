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

package com.moez.QKSMS.email

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.moez.QKSMS.model.EmailAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Gmail OAuth authentication and token lifecycle.
 */
@Singleton
class GmailOAuthManager @Inject constructor(
    private val context: Context
) {
    companion object {
        const val RC_SIGN_IN = 9001
        val GMAIL_SEND_SCOPE = Scope("https://www.googleapis.com/auth/gmail.send")
    }

    private val googleSignInOptions: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(GMAIL_SEND_SCOPE)
            .build()
    }

    private val googleSignInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(context, googleSignInOptions)
    }

    /**
     * Gets the sign-in intent to start the OAuth flow.
     * This should be launched with startActivityForResult.
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Handles the result from the sign-in activity.
     *
     * @param data The intent data from onActivityResult
     * @return The signed-in Google account, or null if sign-in failed
     */
    fun handleSignInResult(data: Intent?): GoogleSignInAccount? {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            Timber.d("GmailOAuthManager: Sign-in successful for ${account?.email}")
            account
        } catch (e: ApiException) {
            Timber.e(e, "GmailOAuthManager: Sign-in failed with code ${e.statusCode}")
            null
        }
    }

    /**
     * Handles the result from the sign-in activity and returns the email.
     * This method avoids exposing GoogleSignInAccount to the presentation layer.
     *
     * @param data The intent data from onActivityResult
     * @return The email address of the signed-in account, or null if sign-in failed
     */
    fun handleSignInResultAndGetEmail(data: Intent?): String? {
        return handleSignInResult(data)?.email
    }

    /**
     * Gets the currently signed-in Google account.
     */
    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    /**
     * Checks if the user has granted the gmail.send scope.
     */
    fun hasGmailSendPermission(): Boolean {
        val account = getSignedInAccount() ?: return false
        return GoogleSignIn.hasPermissions(account, GMAIL_SEND_SCOPE)
    }

    /**
     * Gets a valid OAuth token for the given Gmail account name.
     * Returns null if the user needs to re-authenticate.
     */
    suspend fun getValidToken(gmailAccountName: String?): String? {
        return withContext(Dispatchers.IO) {
            try {
                val googleAccount = getSignedInAccount()

                // Check if the signed-in account matches
                if (googleAccount == null || googleAccount.email != gmailAccountName) {
                    Timber.w("GmailOAuthManager: No matching signed-in account (expected: $gmailAccountName, got: ${googleAccount?.email})")
                    return@withContext null
                }

                // Check if we have the required scope
                if (!GoogleSignIn.hasPermissions(googleAccount, GMAIL_SEND_SCOPE)) {
                    Timber.w("GmailOAuthManager: Missing gmail.send scope")
                    return@withContext null
                }

                // Get a fresh token using GoogleAuthUtil
                val token = getAccessToken(googleAccount)
                Timber.d("GmailOAuthManager: Got access token for $gmailAccountName")
                token

            } catch (e: Exception) {
                Timber.e(e, "GmailOAuthManager: Error getting token")
                null
            }
        }
    }

    /**
     * Gets an access token for the Google account.
     */
    private suspend fun getAccessToken(googleAccount: GoogleSignInAccount): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Use silentSignIn to get a fresh token
                val account = googleSignInClient.silentSignIn().await()

                // The access token is obtained through GoogleAuthUtil
                // For Android, we use the account's serverAuthCode or request token directly
                val scope = "oauth2:${GMAIL_SEND_SCOPE.scopeUri}"

                com.google.android.gms.auth.GoogleAuthUtil.getToken(
                    context,
                    account.account!!,
                    scope
                )
            } catch (e: Exception) {
                Timber.e(e, "GmailOAuthManager: Failed to get access token")
                null
            }
        }
    }

    /**
     * Signs out the current Google account.
     */
    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                googleSignInClient.signOut().await()
                Timber.d("GmailOAuthManager: Signed out successfully")
            } catch (e: Exception) {
                Timber.e(e, "GmailOAuthManager: Error signing out")
            }
        }
    }

    /**
     * Revokes access and signs out.
     */
    suspend fun revokeAccess() {
        withContext(Dispatchers.IO) {
            try {
                googleSignInClient.revokeAccess().await()
                Timber.d("GmailOAuthManager: Access revoked successfully")
            } catch (e: Exception) {
                Timber.e(e, "GmailOAuthManager: Error revoking access")
            }
        }
    }

    /**
     * Checks if the given email account has valid Gmail OAuth credentials.
     */
    fun isAuthorized(account: EmailAccount): Boolean {
        if (account.accountType.uppercase() != "GMAIL") return false

        val googleAccount = getSignedInAccount() ?: return false
        return googleAccount.email == account.gmailAccountName &&
                GoogleSignIn.hasPermissions(googleAccount, GMAIL_SEND_SCOPE)
    }
}
