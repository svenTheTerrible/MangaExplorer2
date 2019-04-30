package com.example.mangaexplorer2.MangaSources

class ChapterResult(val name: String, url: String)

class SearchResult(val name: String, url: String, coverUrl: String)

enum class MangaSourceName{
    MANGATOWN
}

abstract class MangaSource{
    abstract val sourceName: MangaSourceName;
    abstract fun getChapters(): List<ChapterResult>
    abstract fun getSearchResult(): List<SearchResult>
}
