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
package com.moez.QKSMS.common

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.BroadcastReceiver
import androidx.core.provider.FontRequest
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.FontRequestEmojiCompatConfig
import com.fulldive.extension.divesms.R
import com.moez.QKSMS.injection.AppComponentManager
import com.moez.QKSMS.injection.appComponent
import com.moez.QKSMS.manager.AnalyticsManager
import com.moez.QKSMS.manager.ReferralManager
import com.moez.QKSMS.migration.QkMigration
import com.moez.QKSMS.migration.QkRealmMigration
import com.moez.QKSMS.service.ForwardingWorkerHelper
import com.moez.QKSMS.service.ForwardingWorkerHelperImplProvider
import com.moez.QKSMS.util.NightModeManager
import com.uber.rxdogtag.RxDogTag
import com.uber.rxdogtag.autodispose.AutoDisposeConfigurer
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasBroadcastReceiverInjector
import dagger.android.HasServiceInjector
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

//todo urls rate etc
class QKApplication : Application(), HasActivityInjector, HasBroadcastReceiverInjector,
    HasServiceInjector {

    /**
     * Inject these so that they are forced to initialize
     */
    @Suppress("unused")
    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Suppress("unused")
    @Inject
    lateinit var qkMigration: QkMigration

    @Inject
    lateinit var dispatchingActivityInjector: DispatchingAndroidInjector<Activity>
    @Inject
    lateinit var dispatchingBroadcastReceiverInjector: DispatchingAndroidInjector<BroadcastReceiver>
    @Inject
    lateinit var dispatchingServiceInjector: DispatchingAndroidInjector<Service>

    // @Inject lateinit var fileLoggingTree: FileLoggingTree
    @Inject
    lateinit var nightModeManager: NightModeManager
    @Inject
    lateinit var realmMigration: QkRealmMigration
    @Inject
    lateinit var referralManager: ReferralManager

    @Inject
    lateinit var forwardingWorkerHelper: ForwardingWorkerHelperImplProvider

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        Timber.plant(Timber.DebugTree())

        AppComponentManager.init(this)
        appComponent.inject(this)

        // Initialize ForwardingWorkerHelper for email forwarding
        ForwardingWorkerHelper.init(forwardingWorkerHelper)

        //  Multidex.install()
        Realm.init(this)
        Realm.setDefaultConfiguration(
            RealmConfiguration.Builder()
                .allowWritesOnUiThread(true)
                .compactOnLaunch()
                .migration(realmMigration)
                .schemaVersion(QkRealmMigration.SchemaVersion)
                .build()
        )

        qkMigration.performMigration()

        GlobalScope.launch(Dispatchers.IO) {
            referralManager.trackReferrer()
        }

        nightModeManager.updateCurrentTheme()

        val fontRequest = FontRequest(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            "Noto Color Emoji Compat",
            R.array.com_google_android_gms_fonts_certs
        )

        EmojiCompat.init(FontRequestEmojiCompatConfig(this, fontRequest))

        RxDogTag.builder()
            .configureWith(AutoDisposeConfigurer::configure)
            .install()
    }

    override fun activityInjector(): AndroidInjector<Activity> {
        return dispatchingActivityInjector
    }

    override fun broadcastReceiverInjector(): AndroidInjector<BroadcastReceiver> {
        return dispatchingBroadcastReceiverInjector
    }

    override fun serviceInjector(): AndroidInjector<Service> {
        return dispatchingServiceInjector
    }

}