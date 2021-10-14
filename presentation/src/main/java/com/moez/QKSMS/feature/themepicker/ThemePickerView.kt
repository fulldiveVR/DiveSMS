
package com.moez.QKSMS.feature.themepicker

import com.moez.QKSMS.common.base.QkViewContract
import io.reactivex.Observable

interface ThemePickerView : QkViewContract<ThemePickerState> {

    fun themeSelected(): Observable<Int>
    fun hsvThemeSelected(): Observable<Int>
    fun clearHsvThemeClicks(): Observable<*>
    fun applyHsvThemeClicks(): Observable<*>

    fun setCurrentTheme(color: Int)
    fun showQksmsPlusSnackbar()

}