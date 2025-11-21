/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.moez.QKSMS.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import timber.log.Timber
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

/**
 * Result of an image compression operation
 */
sealed class ImageCompressionResult {
    data class Success(
        val bytes: ByteArray,
        val originalSize: Long,
        val compressedSize: Long,
        val compressionRatio: Float,
        val finalWidth: Int,
        val finalHeight: Int,
        val attemptCount: Int
    ) : ImageCompressionResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Success
            if (!bytes.contentEquals(other.bytes)) return false
            return true
        }
        override fun hashCode(): Int = bytes.contentHashCode()
    }

    data class Failure(
        val errorCode: MmsErrorCode,
        val errorMessage: String,
        val originalSize: Long? = null,
        val targetSize: Long? = null
    ) : ImageCompressionResult()
}

object ImageUtils {

    private const val MAX_COMPRESSION_ATTEMPTS = 5
    private const val MIN_QUALITY = 50
    private const val MIN_DIMENSION = 100

    fun getScaledGif(context: Context, uri: Uri, maxWidth: Int, maxHeight: Int, quality: Int = 90): ByteArray {
        val gif = GlideApp
                .with(context)
                .asGif()
                .load(uri)
                .centerInside()
                .encodeQuality(quality)
                .submit(maxWidth, maxHeight)
                .get()

        val outputStream = ByteArrayOutputStream()
        GifEncoder(context, GlideApp.get(context).bitmapPool).encodeTransformedToStream(gif, outputStream)
        return outputStream.toByteArray()
    }

    fun getScaledImage(context: Context, uri: Uri, maxWidth: Int, maxHeight: Int, quality: Int = 90): ByteArray {
        return GlideApp
                .with(context)
                .`as`(ByteArray::class.java)
                .load(uri)
                .centerInside()
                .encodeQuality(quality)
                .submit(maxWidth, maxHeight)
                .get()
    }

    /**
     * Compress an image to fit within the specified size limit.
     * Uses progressive compression with multiple attempts.
     *
     * @param context Android context
     * @param uri URI of the image to compress
     * @param maxBytes Maximum allowed size in bytes
     * @param maxWidth Maximum width (carrier limit)
     * @param maxHeight Maximum height (carrier limit)
     * @param isGif Whether the image is a GIF
     * @return ImageCompressionResult indicating success or failure with details
     */
    fun compressImageToSize(
        context: Context,
        uri: Uri,
        maxBytes: Long,
        maxWidth: Int = Int.MAX_VALUE,
        maxHeight: Int = Int.MAX_VALUE,
        isGif: Boolean = false
    ): ImageCompressionResult {
        try {
            // Get original image dimensions
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            if (originalWidth <= 0 || originalHeight <= 0) {
                return ImageCompressionResult.Failure(
                    errorCode = MmsErrorCode.INVALID_ATTACHMENT,
                    errorMessage = "Unable to read image dimensions"
                )
            }

            // Get original file size
            val originalSize = context.contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0L

            Timber.d("Compressing image: ${originalWidth}x${originalHeight}, ${originalSize / 1024}KB, target: ${maxBytes / 1024}KB")

            val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
            var currentWidth = minOf(originalWidth, maxWidth)
            var currentHeight = minOf(originalHeight, maxHeight)
            var quality = 90
            var attempts = 0
            var scaledBytes: ByteArray

            // First attempt with original or max dimensions
            scaledBytes = try {
                if (isGif) {
                    getScaledGif(context, uri, currentWidth, currentHeight, quality)
                } else {
                    getScaledImage(context, uri, currentWidth, currentHeight, quality)
                }
            } catch (e: Exception) {
                Timber.e(e, "Initial image scaling failed")
                return ImageCompressionResult.Failure(
                    errorCode = MmsErrorCode.COMPRESSION_FAILED,
                    errorMessage = "Failed to process image: ${e.message}",
                    originalSize = originalSize,
                    targetSize = maxBytes
                )
            }

            // Progressive compression loop
            while (scaledBytes.size > maxBytes && attempts < MAX_COMPRESSION_ATTEMPTS) {
                attempts++

                // Calculate new dimensions based on size ratio
                val sizeRatio = maxBytes.toFloat() / scaledBytes.size
                val scaleFactor = sqrt(sizeRatio) * 0.9f // 10% buffer

                if (scaleFactor <= 0 || scaleFactor >= 1) {
                    // Can't scale further, try reducing quality
                    quality = maxOf(MIN_QUALITY, quality - 15)
                } else {
                    currentWidth = maxOf(MIN_DIMENSION, (currentWidth * scaleFactor).toInt())
                    currentHeight = maxOf(MIN_DIMENSION, (currentWidth / aspectRatio).toInt())
                }

                Timber.d("Compression attempt $attempts: ${currentWidth}x${currentHeight}, quality=$quality")

                scaledBytes = try {
                    if (isGif) {
                        getScaledGif(context, uri, currentWidth, currentHeight, quality)
                    } else {
                        getScaledImage(context, uri, currentWidth, currentHeight, quality)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Compression attempt $attempts failed")
                    continue
                }

                Timber.d("Attempt $attempts result: ${scaledBytes.size / 1024}KB")
            }

            // Check if compression succeeded
            return if (scaledBytes.size <= maxBytes) {
                val compressionRatio = if (originalSize > 0) {
                    scaledBytes.size.toFloat() / originalSize.toFloat()
                } else {
                    1f
                }

                Timber.i("Compression successful: ${originalSize / 1024}KB -> ${scaledBytes.size / 1024}KB " +
                        "(${(compressionRatio * 100).toInt()}%) in $attempts attempts")

                ImageCompressionResult.Success(
                    bytes = scaledBytes,
                    originalSize = originalSize,
                    compressedSize = scaledBytes.size.toLong(),
                    compressionRatio = compressionRatio,
                    finalWidth = currentWidth,
                    finalHeight = currentHeight,
                    attemptCount = attempts
                )
            } else {
                Timber.w("Compression failed after $attempts attempts: " +
                        "${scaledBytes.size / 1024}KB > ${maxBytes / 1024}KB target")

                ImageCompressionResult.Failure(
                    errorCode = MmsErrorCode.COMPRESSION_FAILED,
                    errorMessage = "Unable to compress image to fit carrier limits after $attempts attempts. " +
                            "Original: ${originalSize / 1024}KB, Best: ${scaledBytes.size / 1024}KB, Target: ${maxBytes / 1024}KB",
                    originalSize = originalSize,
                    targetSize = maxBytes
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during image compression")
            return ImageCompressionResult.Failure(
                errorCode = MmsErrorCode.UNKNOWN_ERROR,
                errorMessage = "Unexpected error: ${e.message}"
            )
        }
    }

    /**
     * Get image dimensions without loading the full bitmap
     */
    fun getImageDimensions(context: Context, uri: Uri): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            if (options.outWidth > 0 && options.outHeight > 0) {
                Pair(options.outWidth, options.outHeight)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to get image dimensions")
            null
        }
    }

    /**
     * Get file size from URI
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0L
        } catch (e: Exception) {
            Timber.w(e, "Failed to get file size")
            0L
        }
    }
}