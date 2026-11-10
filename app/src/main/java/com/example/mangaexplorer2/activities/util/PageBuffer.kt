package com.example.mangaexplorer2.activities.util

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.mangaexplorer2.mangaSources.util.MangaSource
import com.example.mangaexplorer2.mangaSources.util.PageResult
import java.lang.Exception

enum class PageLoadingState {
    IS_LOADING,
    LOADED,
    FAILED_TO_LOAD
}

enum class PageLoadingError {
    PAGE_NOT_LOADING,
    IMAGE_NOT_LOADING
}

data class PageBufferResult(
    val loadingStatus: PageLoadingState,
    val errorType: PageLoadingError?,
    val pageResult: PageResult?,
    val imageBitmap: List<Bitmap>,
    val pageUrl: String
)


val MAX_LOADING_TRIES = 5

class PageBuffer(
    val timestamp: Long,
    private val context: Context,
    private val mangaSource: MangaSource,
    val pageUrl: String,
    private val chapterMenuUrl: String,
    private val mangaName: String,
    private val nextPageCallback: (nextPageUrl: String?) -> Unit
) {
    private var loadingStatus: PageLoadingState =
        PageLoadingState.IS_LOADING
    private var errorType: PageLoadingError? = null
    private var pageResult: PageResult? = null
    private var imageBitmaps: List<Bitmap> = listOf()

    init {
        Thread(Runnable { loadPage() }).start()
    }

    fun getResult(): PageBufferResult {
        return PageBufferResult(
            loadingStatus,
            errorType,
            pageResult,
            imageBitmaps,
            pageUrl
        )
    }

    fun reloadFailedPage(callback: () -> Unit): Unit {
        val handler = Handler(Looper.getMainLooper())
        Thread {
            loadPage(true)
            handler.post {
                callback()
            }
        }.start()
    }

    private fun tryMultiplePageLoads(): PageResult? {
        var loadingTry = 0
        var pageResult: PageResult? = null
        while (loadingTry < MAX_LOADING_TRIES && pageResult == null) {
            try {
                pageResult = mangaSource.getPageResultSync(pageUrl, chapterMenuUrl, mangaName)
            } catch (e: Exception) {
                Log.e("getPageResultSync", e.message ?: e.localizedMessage ?: "could not load exception text")
                loadingTry += 1
                Thread.sleep(1000)
            }

        }
        return pageResult
    }

    private fun loadPage(noCallback: Boolean = false) {
        pageResult = tryMultiplePageLoads()
        if (pageResult == null) {
            errorType = PageLoadingError.PAGE_NOT_LOADING
            loadingStatus = PageLoadingState.FAILED_TO_LOAD
            if (!noCallback) {
                nextPageCallback(null)
            }
            return
        }
        imageUrlToBitmapList()
    }

    private fun imageUrlToBitmapList(tryCount: Int = 0) {
        val imageUrl = pageResult?.imageUrl
        if (imageUrl == null) {
            errorType = PageLoadingError.IMAGE_NOT_LOADING
            loadingStatus = PageLoadingState.FAILED_TO_LOAD
            return
        }

        val imageFile = PageImageCache.get(context)
            .get(imageUrl, mapOf("Referer" to mangaSource.refererUrl))
        if (imageFile == null) {
            retryImageOrFail(tryCount)
            return
        }

        val screenWidth = context.resources.displayMetrics.widthPixels
        val bitmaps = PageImageSlicer.decodeSlices(imageFile, screenWidth)
        if (bitmaps.isEmpty()) {
            //the downloaded file is probably broken, remove it so the next try can download it again
            PageImageCache.get(context).remove(imageUrl)
            retryImageOrFail(tryCount)
            return
        }

        imageBitmaps = bitmaps
        loadingStatus = PageLoadingState.LOADED
        notifyNextPage(pageResult?.nextPageUrl)
    }

    private fun retryImageOrFail(tryCount: Int) {
        if (tryCount < MAX_LOADING_TRIES) {
            Thread.sleep(1000)
            imageUrlToBitmapList(tryCount + 1)
            return
        }
        errorType = PageLoadingError.IMAGE_NOT_LOADING
        loadingStatus = PageLoadingState.FAILED_TO_LOAD
    }

    /**
     * Hands the image of this page back to the disk cache, so the images of pages that are not
     * needed anymore do not pile up. Glide's cache is not used here anymore, so nothing else is affected.
     */
    fun clearCachedImage() {
        pageResult?.imageUrl?.let { PageImageCache.get(context).remove(it) }
    }

    private fun notifyNextPage(nextPageUrl: String?) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            nextPageCallback(nextPageUrl)
        }
    }
}