package com.example.mangaexplorer2.MangaSources

class MangaTown():MangaSource() {

    override val sourceName: MangaSourceName = MangaSourceName.MANGATOWN

    override fun getChapters(): List<ChapterResult> {
        return listOf<ChapterResult>(
            ChapterResult(
                name = "test",
                url = "aslalala"
            )
        )
    }

    override fun getSearchResult(): List<SearchResult>{
        return listOf<SearchResult>(
            SearchResult(
                name= "test2",
                url="test3",
                coverUrl = "test4"
            )
        )
    }
}