
package com.moez.QKSMS.feature.themepicker.injection

import com.moez.QKSMS.feature.themepicker.ThemePickerController
import com.moez.QKSMS.injection.scope.ControllerScope
import dagger.Subcomponent

@ControllerScope
@Subcomponent(modules = [ThemePickerModule::class])
interface ThemePickerComponent {

    fun inject(controller: ThemePickerController)

    @Subcomponent.Builder
    interface Builder {
        fun themePickerModule(module: ThemePickerModule): Builder
        fun build(): ThemePickerComponent
    }

}