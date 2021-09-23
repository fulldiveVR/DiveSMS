
package com.moez.QKSMS.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import java.io.ByteArrayOutputStream

object ImageUtils {

    fun compressGif(context: Context, uri: Uri, maxBytes: Int): ByteArray {
        val request = GlideApp
                .with(context)
                .asGif()
                .load(uri)
                .transform(CenterCrop())

        val gif = request.submit().get()

        val width = gif.firstFrame.width
        val height = gif.firstFrame.height

        val stream = ByteArrayOutputStream()
        GifEncoder(context, GlideApp.get(context).bitmapPool)
                .encodeTransformedToStream(gif, stream)

        val unscaledBytes = stream.size().toDouble()

        var attempts = 0
        var bytes = unscaledBytes
        while (maxBytes > 0 && bytes > maxBytes) {
            val scale = Math.sqrt(maxBytes / unscaledBytes) * (1 - attempts * 0.1)

            val scaledGif = request.submit((width * scale).toInt(), (height * scale).toInt()).get()

            stream.reset()
            GifEncoder(context, GlideApp.get(context).bitmapPool)
                    .encodeTransformedToStream(scaledGif, stream)

            attempts++
            bytes = stream.size().toDouble()
        }

        return stream.toByteArray()
    }

    fun compressBitmap(src: Bitmap, maxBytes: Int): ByteArray {
        val quality = 90

        val height = src.height
        val width = src.width

        val stream = ByteArrayOutputStream()
        src.compress(Bitmap.CompressFormat.JPEG, quality, stream)

        val unscaledBytes = stream.size().toDouble()

        // Based on the byte size of the bitmap, we'll try to reduce the image's dimensions such
        // that it will fit within the max byte size set. If we don't get it right the first time,
        // use a slightly heavier compression until we fit within the max size
        var attempts = 0
        var bytes = unscaledBytes
        while (maxBytes > 0 && bytes > maxBytes) {
            val scale = Math.sqrt(maxBytes / unscaledBytes) * (1 - attempts * 0.1)

            stream.reset()
            Bitmap.createScaledBitmap(src, (width * scale).toInt(), (height * scale).toInt(), true)
                    .compress(Bitmap.CompressFormat.JPEG, quality, stream)

            attempts++
            bytes = stream.size().toDouble()
        }

        return stream.toByteArray().also { src.recycle() }
    }

}