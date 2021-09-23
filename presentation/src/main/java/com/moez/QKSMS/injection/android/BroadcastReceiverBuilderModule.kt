
package com.moez.QKSMS.injection.android

import com.moez.QKSMS.feature.widget.WidgetProvider
import com.moez.QKSMS.injection.scope.ActivityScope
import com.moez.QKSMS.receiver.BootReceiver
import com.moez.QKSMS.receiver.DefaultSmsChangedReceiver
import com.moez.QKSMS.receiver.DeleteMessagesReceiver
import com.moez.QKSMS.receiver.MarkReadReceiver
import com.moez.QKSMS.receiver.MarkSeenReceiver
import com.moez.QKSMS.receiver.MmsReceivedReceiver
import com.moez.QKSMS.receiver.MmsReceiver
import com.moez.QKSMS.receiver.MmsSentReceiver
import com.moez.QKSMS.receiver.MmsUpdatedReceiver
import com.moez.QKSMS.receiver.NightModeReceiver
import com.moez.QKSMS.receiver.RemoteMessagingReceiver
import com.moez.QKSMS.receiver.SendScheduledMessageReceiver
import com.moez.QKSMS.receiver.SmsDeliveredReceiver
import com.moez.QKSMS.receiver.SmsProviderChangedReceiver
import com.moez.QKSMS.receiver.SmsReceiver
import com.moez.QKSMS.receiver.SmsSentReceiver
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class BroadcastReceiverBuilderModule {

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindBootReceiver(): BootReceiver

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindDefaultSmsChangedReceiver(): DefaultSmsChangedReceiver

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindDeleteMessagesReceiver(): DeleteMessagesReceiver

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindMarkReadReceiver(): MarkReadReceiver

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindMarkSeenReceiver(): MarkSeenReceiver

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindMmsReceivedReceiver(): MmsReceivedReceiver

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindMmsReceiver(): MmsReceiver

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindMmsSentReceiver(): MmsSentReceiver

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindMmsUpdatedReceiver(): MmsUpdatedReceiver

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindNightModeReceiver(): NightModeReceiver

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindRemoteMessagingReceiver(): RemoteMessagingReceiver

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindSendScheduledMessageReceiver(): SendScheduledMessageReceiver

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindSmsDeliveredReceiver(): SmsDeliveredReceiver

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindSmsProviderChangedReceiver(): SmsProviderChangedReceiver

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindSmsReceiver(): SmsReceiver

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindSmsSentReceiver(): SmsSentReceiver

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindWidgetProvider(): WidgetProvider

}