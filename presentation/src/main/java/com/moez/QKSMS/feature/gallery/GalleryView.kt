
package com.moez.QKSMS.feature.gallery

import com.moez.QKSMS.common.base.QkView
import com.moez.QKSMS.model.MmsPart
import io.reactivex.Observable

interface GalleryView : QkView<GalleryState> {

    fun optionsItemSelected(): Observable<Int>
    fun screenTouched(): Observable<*>
    fun pageChanged(): Observable<MmsPart>

    fun requestStoragePermission()

}