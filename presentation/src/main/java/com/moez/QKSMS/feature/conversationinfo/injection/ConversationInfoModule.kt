
package com.moez.QKSMS.feature.conversationinfo.injection

import com.moez.QKSMS.feature.conversationinfo.ConversationInfoController
import com.moez.QKSMS.injection.scope.ControllerScope
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class ConversationInfoModule(private val controller: ConversationInfoController) {

    @Provides
    @ControllerScope
    @Named("threadId")
    fun provideThreadId(): Long = controller.threadId

}