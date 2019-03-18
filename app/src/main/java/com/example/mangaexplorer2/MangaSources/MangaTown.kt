package com.example.mangaexplorer2.MangaSources

class MangaTown():MangaSource() {

    override val sourceName: String = "mangatown"

    override fun getChapters(): List<ChapterResult> {
        return listOf<ChapterResult>(
            ChapterResult(
                name = "test",
                url = "aslalala"
            )
        )
    }

    public override fun getSearchResult(): List<SearchResult>{
        return listOf<SearchResult>(
            SearchResult(
                name= "test2",
                url="test3",
                coverUrl = "test4"
            )
        )
    }
}