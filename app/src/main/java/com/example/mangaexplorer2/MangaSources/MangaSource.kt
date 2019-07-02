package com.example.mangaexplorer2.MangaSources

import android.os.AsyncTask
import java.io.Serializable

class ChapterResult(val name: String, val url: String)

class SearchResult(val name: String, val url: String, val coverUrl: String): Serializable

enum class MangaSourceName{
    MANGATOWN
}

abstract class MangaSource: Serializable{
    abstract val sourceName: MangaSourceName;
    abstract fun getChapters(chapterMenuUrl: String, callback:(chapters: List<ChapterResult>) -> Unit): Unit
    abstract fun getSearchResult(searchterm: String, callback:(searchResults: List<SearchResult>)-> Unit): Unit
}

class AsyncWrapper(val asyncTask: ()-> Unit ) : AsyncTask<Void, Void, Void>() {
    override fun doInBackground(vararg params: Void?): Void? {
        asyncTask()
        return null
    }
}
