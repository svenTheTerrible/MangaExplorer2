package com.example.mangaexplorer2.MangaSources

import org.jsoup.Jsoup

class MangaTown() : MangaSource() {

    override val sourceName: MangaSourceName = MangaSourceName.MANGATOWN

    override fun getChapters(): List<ChapterResult> {
        return listOf(
            ChapterResult(
                name = "test",
                url = "aslalala"
            )
        )
    }

    override fun getSearchResult(searchterm: String, callback: (searchResults: List<SearchResult>)-> Unit): Unit {
        AsyncWrapper{
            val doc = Jsoup.connect("https://www.mangatown.com/search.php?name=$searchterm").get()
            callback(
                doc.select(".manga_cover").map { resultItem ->
                    SearchResult(
                        name = resultItem.attr("title"),
                        coverUrl = resultItem.getElementsByTag("img").attr("src"),
                        url = resultItem.attr("href")
                    )
                }
            )
        }.execute()
    }
}