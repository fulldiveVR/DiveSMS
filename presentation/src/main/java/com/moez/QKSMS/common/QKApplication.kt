
package com.moez.QKSMS.common

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.BroadcastReceiver
import androidx.core.provider.FontRequest
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.FontRequestEmojiCompatConfig
import com.moez.QKSMS.R
import com.moez.QKSMS.common.util.CrashlyticsTree
import com.moez.QKSMS.common.util.FileLoggingTree
import com.moez.QKSMS.injection.AppComponentManager
import com.moez.QKSMS.injection.appComponent
import com.moez.QKSMS.manager.AnalyticsManager
import com.moez.QKSMS.migration.QkMigration
import com.moez.QKSMS.migration.QkRealmMigration
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
import timber.log.Timber
import javax.inject.Inject

class QKApplication : Application(), HasActivityInjector, HasBroadcastReceiverInjector, HasServiceInjector {

    /**
     * Inject these so that they are forced to initialize
     */
    @Suppress("unused")
    @Inject lateinit var analyticsManager: AnalyticsManager
    @Suppress("unused")
    @Inject lateinit var qkMigration: QkMigration

    @Inject lateinit var dispatchingActivityInjector: DispatchingAndroidInjector<Activity>
    @Inject lateinit var dispatchingBroadcastReceiverInjector: DispatchingAndroidInjector<BroadcastReceiver>
    @Inject lateinit var dispatchingServiceInjector: DispatchingAndroidInjector<Service>
    @Inject lateinit var fileLoggingTree: FileLoggingTree
    @Inject lateinit var nightModeManager: NightModeManager

    override fun onCreate() {
        super.onCreate()

        Realm.init(this)
        Realm.setDefaultConfiguration(RealmConfiguration.Builder()
                .compactOnLaunch()
                .migration(QkRealmMigration())
                .schemaVersion(QkRealmMigration.SCHEMA_VERSION)
            .allowWritesOnUiThread(true)
                .build())

        AppComponentManager.init(this)
        appComponent.inject(this)

        packageManager.getInstallerPackageName(packageName)?.let { installer ->
            analyticsManager.setUserProperty("Installer", installer)
        }

        nightModeManager.updateCurrentTheme()

        val fontRequest = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs)

        EmojiCompat.init(FontRequestEmojiCompatConfig(this, fontRequest))

        Timber.plant(Timber.DebugTree(), CrashlyticsTree(), fileLoggingTree)

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