package com.example.mangaexplorer2.MangaSources

import org.jsoup.Jsoup

class MangaTown() : MangaSource() {

    override val sourceName: MangaSourceName = MangaSourceName.MANGATOWN

    override fun getChapters(chapterMenuUrl: String, callback:(chapters: List<ChapterResult>)->Unit): Unit {
        AsyncWrapper{
            val doc = Jsoup.connect(chapterMenuUrl).get()
            callback(
                doc.select(".chapter_list").select("a").map{chapterLink->
                    ChapterResult(
                        name = chapterLink.text(),
                        url = repairUrl(chapterLink.attr("href"))
                    )
                }
            )
        }.execute()
    }

    private fun repairUrl(url: String): String {
        return "https:" + url
    }

    override fun getSearchResult(searchterm: String, callback: (searchResults: List<SearchResult>)-> Unit): Unit {
        AsyncWrapper{
            val doc = Jsoup.connect("https://www.mangatown.com/search.php?name=$searchterm").get()
            callback(
                doc.select(".manga_cover").map { resultItem ->
                    SearchResult(
                        name = resultItem.attr("title"),
                        coverUrl = resultItem.getElementsByTag("img").attr("src"),
                        url = repairUrl(resultItem.attr("href"))
                    )
                }
            )
        }.execute()
    }
}