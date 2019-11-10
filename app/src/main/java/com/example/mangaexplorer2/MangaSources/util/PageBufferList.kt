package com.example.mangaexplorer2.MangaSources.util

import android.content.Context
import android.os.Handler

class PageBufferList(private val context: Context, private val pageBufferSize: Int) {
    private var pageBufferList = mutableListOf<PageBuffer>()

    fun createPageBuffer(
        pageUrl: String,
        chapterMenuUrl: String,
        mangaSource: MangaSource,
        remainingBufferSize: Int = pageBufferSize
    ) {
        val matchingEntry = pageBufferList.find { it.pageUrl == pageUrl }

        fun loadNextPage(nextPageUrl: String?) {
            if (remainingBufferSize > 0 && nextPageUrl != null) {
                createPageBuffer(nextPageUrl, chapterMenuUrl, mangaSource, remainingBufferSize - 1)
            }
        }

        if (matchingEntry == null) {
            val pageBuffer = PageBuffer(context, mangaSource, pageUrl, chapterMenuUrl) {
                loadNextPage(it)
            }
            pageBufferList.add(pageBuffer)
        } else {
            if (remainingBufferSize > 0 && matchingEntry.getResult().pageResult?.nextPageUrl != null) {
                loadNextPage(matchingEntry.getResult().pageResult!!.nextPageUrl!!)
            }
        }
    }

    fun getPageResult(pageUrl: String, callback: (pageResultBuffer: PageBufferResult?) -> Unit) {
        val matchingEntry = pageBufferList.find { it.pageUrl == pageUrl } ?: return callback(null)
        val handler = Handler()
        Thread(Runnable {
            while (matchingEntry.getResult().loadingStatus == PageLoadingState.IS_LOADING) {
                Thread.sleep(1000)
            }
            handler.post {
                callback(matchingEntry.getResult())
            }
        }).start()
    }
}