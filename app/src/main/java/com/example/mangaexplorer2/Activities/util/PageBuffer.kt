package com.example.mangaexplorer2.Activities.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mangaexplorer2.GlideApp
import com.example.mangaexplorer2.MangaSources.util.MangaSource
import com.example.mangaexplorer2.MangaSources.util.PageResult
import java.lang.Exception

enum class PageLoadingState{
    IS_LOADING,
    LOADED,
    FAILED_TO_LOAD
}

enum class PageLoadingError{
    PAGE_NOT_LOADING,
    IMAGE_NOT_LOADING
}

data class PageBufferResult(val loadingStatus: PageLoadingState, val errorType: PageLoadingError?, val pageResult: PageResult?, val imageBitmap: List<Bitmap>)

class PageBuffer(val timestamp: Long, private val context: Context, private val mangaSource: MangaSource, val pageUrl: String, private val chapterMenuUrl: String, private val nextPageCallback:(nextPageUrl: String?)->Unit){
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
            imageBitmaps
        )
    }

    private fun loadPage() {
        try {
            pageResult = mangaSource.getPageResultSync(pageUrl, chapterMenuUrl)
            nextPageCallback(pageResult?.nextPageUrl)
        }catch (e: Exception){
            errorType = PageLoadingError.PAGE_NOT_LOADING
            loadingStatus = PageLoadingState.FAILED_TO_LOAD
            nextPageCallback(null)
            return
        }
        imageUrlToBitmapList()
    }

    private fun imageUrlToBitmapList(){
        val imageUrl = pageResult?.imageUrl
        if(imageUrl == null){
            errorType = PageLoadingError.PAGE_NOT_LOADING
            loadingStatus = PageLoadingState.FAILED_TO_LOAD
            return
        }

        GlideApp.with(context)
            .asBitmap()
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .load(Uri.parse(imageUrl))
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(p0: Drawable?) {}
                override fun onResourceReady(p0: Bitmap, p1: Transition<in Bitmap>?) {
                    imageBitmaps = if (p0.height / p0.width > 2.5) {
                        splitBitmaps(p0)
                    } else {
                        listOf(p0)
                    }
                    loadingStatus = PageLoadingState.LOADED
                }
            })
    }

    private fun splitBitmaps(origBitmap: Bitmap): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        val origHeight = origBitmap.height
        var processedHeight = 0
        while (processedHeight < origHeight) {
            val restHeight = origHeight - processedHeight
            val heightToUse = if (restHeight < 200) restHeight else 200
            bitmaps.add(Bitmap.createBitmap(origBitmap, 0, processedHeight, origBitmap.width, heightToUse))
            processedHeight += 200
        }
        return bitmaps
    }
}