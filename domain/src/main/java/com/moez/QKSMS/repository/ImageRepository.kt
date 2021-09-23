
package com.moez.QKSMS.repository

import android.graphics.Bitmap
import android.net.Uri

interface ImageRepository {

    fun loadImage(uri: Uri, width: Int, height: Int): Bitmap?

}
