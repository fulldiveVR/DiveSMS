
package com.moez.QKSMS.feature.conversationinfo.injection

import com.moez.QKSMS.feature.conversationinfo.ConversationInfoController
import com.moez.QKSMS.injection.scope.ControllerScope
import dagger.Subcomponent

@ControllerScope
@Subcomponent(modules = [ConversationInfoModule::class])
interface ConversationInfoComponent {

    fun inject(controller: ConversationInfoController)

    @Subcomponent.Builder
    interface Builder {
        fun conversationInfoModule(module: ConversationInfoModule): Builder
        fun build(): ConversationInfoComponent
    }

}