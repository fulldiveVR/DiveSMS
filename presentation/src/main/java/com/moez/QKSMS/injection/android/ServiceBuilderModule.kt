
package com.moez.QKSMS.injection.android

import com.moez.QKSMS.feature.backup.RestoreBackupService
import com.moez.QKSMS.injection.scope.ActivityScope
import com.moez.QKSMS.service.HeadlessSmsSendService
import com.moez.QKSMS.receiver.SendSmsReceiver
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ServiceBuilderModule {

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindHeadlessSmsSendService(): HeadlessSmsSendService

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindRestoreBackupService(): RestoreBackupService

    @ActivityScope
    @ContributesAndroidInjector()
    abstract fun bindSendSmsReceiver(): SendSmsReceiver

}