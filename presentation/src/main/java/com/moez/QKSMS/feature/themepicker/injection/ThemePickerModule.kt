
package com.moez.QKSMS.feature.themepicker.injection

import com.moez.QKSMS.feature.themepicker.ThemePickerController
import com.moez.QKSMS.injection.scope.ControllerScope
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class ThemePickerModule(private val controller: ThemePickerController) {

    @Provides
    @ControllerScope
    @Named("threadId")
    fun provideThreadId(): Long = controller.threadId

}