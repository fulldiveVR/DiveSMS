
package com.moez.QKSMS.feature.gallery

import com.moez.QKSMS.model.MmsPart
import io.realm.RealmResults

data class GalleryState(
    val navigationVisible: Boolean = true,
    val title: String? = "",
    val parts: RealmResults<MmsPart>? = null
)
