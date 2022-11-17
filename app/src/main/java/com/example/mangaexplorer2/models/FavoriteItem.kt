package com.example.mangaexplorer2.models

enum class NextChapterState {
    UNAVAILABLE, AVAILABLE, ERROR
}

class FavoriteItem(val mangaTitle: String, val mangaSource:String, val coverImageUrl: String, val chapterMenuUrl: String, val hasNewChapter: NextChapterState, val currentPageUrl: String){}