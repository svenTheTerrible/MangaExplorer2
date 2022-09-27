package com.example.mangaexplorer2.Activities.util

import android.content.Context
import android.os.Handler
import com.example.mangaexplorer2.MangaSources.util.MangaSource

class PageBufferList(private val context: Context, private val pageBufferSize: Int) {
    private var pageBufferList = mutableListOf<PageBuffer>()

    fun createPageBuffer(
        pageUrl: String,
        chapterMenuUrl: String,
        mangaSource: MangaSource,
        mangaName: String,
        remainingBufferSize: Int = pageBufferSize
    ) {
        clearOldBuffers()
        val matchingEntry = pageBufferList.find { it.pageUrl == pageUrl }

        fun loadNextPage(nextPageUrl: String?) {
            if (remainingBufferSize > 0 && nextPageUrl != null) {
                createPageBuffer(nextPageUrl, chapterMenuUrl, mangaSource, mangaName, remainingBufferSize - 1)
            }
        }

        if (matchingEntry == null) {
            val pageBuffer =
                PageBuffer(System.currentTimeMillis(), context, mangaSource, pageUrl, chapterMenuUrl, mangaName) {
                    loadNextPage(it)
                }
            pageBufferList.add(pageBuffer)
        } else {
            if (remainingBufferSize > 0 && matchingEntry.getResult().pageResult?.nextPageUrl != null) {
                loadNextPage(matchingEntry.getResult().pageResult!!.nextPageUrl!!)
            }
        }
    }

    fun getPageResult(pageUrl: String, callback: (pageResultBuffer: PageBuffer?) -> Unit) {
        val matchingEntry = pageBufferList.find { it.pageUrl == pageUrl } ?: return callback(null)
        val handler = Handler()
        Thread(Runnable {
            while (matchingEntry.getResult().loadingStatus == PageLoadingState.IS_LOADING) {
                Thread.sleep(1000)
            }
            handler.post {
                callback(matchingEntry)
            }
        }).start()
    }

    private fun clearOldBuffers(){
        val threeMinutes = 1000*60*3
        pageBufferList = pageBufferList.filter { it.getResult().loadingStatus == PageLoadingState.IS_LOADING || System.currentTimeMillis() - it.timestamp < threeMinutes}.toMutableList()
    }
}