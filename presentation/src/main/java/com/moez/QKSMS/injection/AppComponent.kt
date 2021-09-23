
package com.moez.QKSMS.injection

import com.moez.QKSMS.common.QKApplication
import com.moez.QKSMS.common.QkDialog
import com.moez.QKSMS.common.util.QkChooserTargetService
import com.moez.QKSMS.common.widget.AvatarView
import com.moez.QKSMS.common.widget.PagerTitleView
import com.moez.QKSMS.common.widget.PreferenceView
import com.moez.QKSMS.common.widget.QkEditText
import com.moez.QKSMS.common.widget.QkSwitch
import com.moez.QKSMS.common.widget.QkTextView
import com.moez.QKSMS.common.widget.RadioPreferenceView
import com.moez.QKSMS.feature.backup.BackupController
import com.moez.QKSMS.feature.blocking.BlockingController
import com.moez.QKSMS.feature.blocking.manager.BlockingManagerController
import com.moez.QKSMS.feature.blocking.messages.BlockedMessagesController
import com.moez.QKSMS.feature.blocking.numbers.BlockedNumbersController
import com.moez.QKSMS.feature.compose.DetailedChipView
import com.moez.QKSMS.feature.conversationinfo.injection.ConversationInfoComponent
import com.moez.QKSMS.feature.settings.SettingsController
import com.moez.QKSMS.feature.settings.about.AboutController
import com.moez.QKSMS.feature.settings.swipe.SwipeActionsController
import com.moez.QKSMS.feature.themepicker.injection.ThemePickerComponent
import com.moez.QKSMS.feature.widget.WidgetAdapter
import com.moez.QKSMS.injection.android.ActivityBuilderModule
import com.moez.QKSMS.injection.android.BroadcastReceiverBuilderModule
import com.moez.QKSMS.injection.android.ServiceBuilderModule
import com.moez.QKSMS.util.ContactImageLoader
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AndroidSupportInjectionModule::class,
    AppModule::class,
    ActivityBuilderModule::class,
    BroadcastReceiverBuilderModule::class,
    ServiceBuilderModule::class])
interface AppComponent {

    fun conversationInfoBuilder(): ConversationInfoComponent.Builder
    fun themePickerBuilder(): ThemePickerComponent.Builder

    fun inject(application: QKApplication)

    fun inject(controller: AboutController)
    fun inject(controller: BackupController)
    fun inject(controller: BlockedMessagesController)
    fun inject(controller: BlockedNumbersController)
    fun inject(controller: BlockingController)
    fun inject(controller: BlockingManagerController)
    fun inject(controller: SettingsController)
    fun inject(controller: SwipeActionsController)

    fun inject(dialog: QkDialog)

    fun inject(fetcher: ContactImageLoader.ContactImageFetcher)

    fun inject(service: WidgetAdapter)

    /**
     * This can't use AndroidInjection, or else it will crash on pre-marshmallow devices
     */
    fun inject(service: QkChooserTargetService)

    fun inject(view: AvatarView)
    fun inject(view: DetailedChipView)
    fun inject(view: PagerTitleView)
    fun inject(view: PreferenceView)
    fun inject(view: RadioPreferenceView)
    fun inject(view: QkEditText)
    fun inject(view: QkSwitch)
    fun inject(view: QkTextView)

}