package com.example.mangaexplorer2.MangaSources.util

import android.os.AsyncTask
import java.io.Serializable

class ChapterResult(val name: String, val url: String): Serializable

class SearchResult(val name: String, val url: String, val coverUrl: String): Serializable

class PageResult(val imageUrl: String?, val pageCount: Int?, val pageAmount: Int? , val chapterName: String?, val nextPageUrl: String?)

enum class MangaSourceName{
    MANGATOWN,
    TENMANGA,
    BATO
}

abstract class MangaSource: Serializable{
    abstract val sourceName: MangaSourceName;
    abstract fun getChapters(chapterMenuUrl: String, callback:(chapters: List<ChapterResult>) -> Unit): Unit
    abstract fun getSearchResult(searchterm: String, callback:(searchResults: List<SearchResult>)-> Unit): Unit
    abstract fun getPageResult(pageUrl: String,chapterMenuUrl: String, callback:(pageResult: PageResult)-> Unit): Unit
    abstract fun getPageResultSync(pageUrl: String,chapterMenuUrl: String): PageResult
}

class AsyncWrapper(val asyncTask: ()-> Unit ) : AsyncTask<Void, Void, Void>() {
    override fun doInBackground(vararg params: Void?): Void? {
        asyncTask()
        return null
    }
}
