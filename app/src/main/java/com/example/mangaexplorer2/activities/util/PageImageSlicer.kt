package com.example.mangaexplorer2.activities.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * Loads a page image from a file and cuts it into vertical slices.
 *
 * A full manga page easily needs a few hundred megabytes once it is decoded, so the image is
 * downsampled to the width of the screen first and afterwards decoded slice by slice. While
 * slicing, only one slice is decoded at a time, which keeps the peak memory usage small instead of
 * holding the whole page in the ram.
 *
 * Every method has to be called from a background thread.
 */
object PageImageSlicer {
    private const val TAG = "PageImageSlicer"

    //pages that are higher than this multiple of their width get sliced
    private const val SLICE_TRIGGER_RATIO = 2.5f
    private const val MIN_SLICE_HEIGHT = 256
    private const val MAX_SLICE_HEIGHT = 2048

    //rgb_565 needs two bytes per pixel, that is the format glide used for these images before as well
    private const val BYTES_PER_PIXEL = 2
    private const val MAX_SLICE_BYTES = 6L * 1024 * 1024

    /**
     * Decodes [image] into the list of bitmaps that are displayed underneath each other.
     *
     * @param targetWidth the width of the screen, the image is downsampled to
     * @return the slices of the image or an empty list when the image could not be decoded
     */
    fun decodeSlices(image: File, targetWidth: Int): List<Bitmap> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(image.path, bounds)
        val sourceWidth = bounds.outWidth
        val sourceHeight = bounds.outHeight
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            Log.e(TAG, "Could not read the image dimensions of '${image.path}'")
            return emptyList()
        }

        val sampleSize = calculateSampleSize(sourceWidth, sourceHeight, targetWidth)
        val width = sourceWidth / sampleSize
        val height = sourceHeight / sampleSize

        if (height <= width * SLICE_TRIGGER_RATIO) {
            return decodeWholeImage(image, sampleSize, slice = false)
        }
        if (!supportsRegionDecoding(image)) {
            Log.d(TAG, "'${image.name}' can not be decoded in slices, loading it at once")
            return decodeWholeImage(image, sampleSize, slice = true)
        }

        return try {
            decodeRegions(image, sampleSize, sourceWidth, sourceHeight)
        } catch (e: Exception) {
            Log.e(TAG, "Decoding '${image.path}' in slices failed", e)
            decodeWholeImage(image, sampleSize, slice = true)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Decoding '${image.path}' in slices ran out of memory", e)
            decodeWholeImage(image, sampleSize * 2, slice = true)
        }
    }

    /**
     * Decodes the image piece by piece straight out of the file, so the whole page is never decoded
     * into one big bitmap.
     */
    @Suppress("DEPRECATION")
    private fun decodeRegions(
        image: File,
        sampleSize: Int,
        sourceWidth: Int,
        sourceHeight: Int
    ): List<Bitmap> {
        val sliceHeight = sliceHeightFor(sourceWidth / sampleSize)
        val sourceSliceHeight = sliceHeight * sampleSize
        val sliceAmount = (sourceHeight + sourceSliceHeight - 1) / sourceSliceHeight
        //spread the leftover over all slices instead of adding one tiny slice at the end
        val regionHeight = (sourceHeight + sliceAmount - 1) / sliceAmount

        image.inputStream().use { input ->
            val decoder = BitmapRegionDecoder.newInstance(input, false)
                ?: throw IOException("The image can not be decoded in slices")
            val slices = mutableListOf<Bitmap>()
            try {
                var top = 0
                while (top < sourceHeight) {
                    val bottom = minOf(top + regionHeight, sourceHeight)
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    slices.add(
                        decoder.decodeRegion(Rect(0, top, sourceWidth, bottom), options)
                            ?: throw IOException("The slice from $top to $bottom could not be decoded")
                    )
                    top = bottom
                }
                return slices
            } catch (e: Exception) {
                //throw away everything that was already decoded before the caller falls back
                recycleAll(slices)
                throw e
            } finally {
                decoder.recycle()
            }
        }
    }

    /**
     * Decodes the whole image at once and cuts the result by hand. Used for formats that can not be
     * decoded in regions and as a fallback.
     */
    private fun decodeWholeImage(image: File, sampleSize: Int, slice: Boolean): List<Bitmap> {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = try {
            BitmapFactory.decodeFile(image.path, options)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Decoding '${image.path}' ran out of memory", e)
            null
        }
        if (bitmap == null) {
            Log.e(TAG, "Could not decode '${image.path}'")
            return emptyList()
        }
        if (!slice) {
            return listOf(bitmap)
        }

        val slices = mutableListOf<Bitmap>()
        try {
            val width = bitmap.width
            val height = bitmap.height
            val sliceHeight = sliceHeightFor(width)
            val sliceAmount = (height + sliceHeight - 1) / sliceHeight
            val chunkHeight = (height + sliceAmount - 1) / sliceAmount
            var processedHeight = 0
            while (processedHeight < height) {
                val heightToUse = minOf(chunkHeight, height - processedHeight)
                slices.add(Bitmap.createBitmap(bitmap, 0, processedHeight, width, heightToUse))
                processedHeight += heightToUse
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Cutting '${image.path}' into slices ran out of memory", e)
            recycleAll(slices)
            return listOf(bitmap)
        }
        bitmap.recycle()
        return slices
    }

    private fun calculateSampleSize(sourceWidth: Int, sourceHeight: Int, targetWidth: Int): Int {
        var sampleSize = 1
        while (sourceWidth / (sampleSize * 2) >= targetWidth && sourceHeight / (sampleSize * 2) > 0) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /** Chooses a slice height that keeps a single slice below [MAX_SLICE_BYTES]. */
    private fun sliceHeightFor(width: Int): Int {
        val maxHeight = (MAX_SLICE_BYTES / BYTES_PER_PIXEL / width).toInt()
        return maxHeight.coerceIn(MIN_SLICE_HEIGHT, MAX_SLICE_HEIGHT)
    }

    /** Region decoding works for jpeg, png and webp images, everything else has to be decoded at once. */
    private fun supportsRegionDecoding(image: File): Boolean {
        return try {
            image.inputStream().use { input ->
                val header = ByteArray(12)
                if (input.read(header) < 4) {
                    return false
                }
                val isJpeg = header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte()
                val isPng = header[0] == 0x89.toByte() && header[1] == 0x50.toByte() &&
                        header[2] == 0x4E.toByte() && header[3] == 0x47.toByte()
                val isWebp = String(header, 0, 4, Charsets.ISO_8859_1) == "RIFF" &&
                        String(header, 8, 4, Charsets.ISO_8859_1) == "WEBP"
                isJpeg || isPng || isWebp
            }
        } catch (e: IOException) {
            Log.e(TAG, "Could not read the file header of '${image.path}'", e)
            false
        }
    }

    private fun recycleAll(bitmaps: List<Bitmap>) {
        for (bitmap in bitmaps) {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
}
