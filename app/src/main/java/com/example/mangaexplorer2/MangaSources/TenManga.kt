package com.example.mangaexplorer2.MangaSources

import com.example.mangaexplorer2.MangaSources.util.*
import org.jsoup.Jsoup

class TenManga() : MangaSource() {

    override val sourceName: MangaSourceName = MangaSourceName.TENMANGA

    override fun getSearchResult(searchterm: String, callback: (searchResults: List<SearchResult>) -> Unit): Unit {
        AsyncWrapper {
            val doc = Jsoup.connect("https://my.tenmanga.com/search/es/?wd=$searchterm").get()
            callback(
                doc.select("#list_container").select("li").map { resultItem ->
                    val dt = resultItem.select("dt")
                    SearchResult(
                        name = dt.select("a").attr("title"),
                        coverUrl = dt.select("img").attr("src"),
                        url = dt.select("a").attr("href")
                    )
                }
            )
        }.execute()
    }

    override fun getChapters(chapterMenuUrl: String, callback: (chapters: List<ChapterResult>) -> Unit): Unit {
        AsyncWrapper {
            val doc = Jsoup.connect(chapterMenuUrl + "?waring=1").get()
            callback(
                doc.select(".chapter-box").select("li").map { chapterItem ->
                    val shortChapter = chapterItem.select(".short")
                    ChapterResult(
                        name = shortChapter.select("a").text(),
                        url = shortChapter.select("a").attr("href")
                    )
                }.reversed()
            )
        }.execute()
    }

    override fun getPageResult(pageUrl: String, chapterMenuUrl: String, callback: (pageResult: PageResult) -> Unit) {
        AsyncWrapper {
            callback(getPageResultSync(pageUrl, chapterMenuUrl))
        }.execute()
    }

    override fun getPageResultSync(pageUrl: String, chapterMenuUrl: String): PageResult {
        val doc = Jsoup.connect(pageUrl).get()
        val image = doc.selectFirst("#manga_pic_1")
        val selectedPageAndPageAmountText = doc.selectFirst(".pic_download").selectFirst("a").text()
        val pageAmountText = selectedPageAndPageAmountText.split(" of ")
        val pageAmount = if (pageAmountText.size > 1) pageAmountText.get(1).toInt() else 0
        val pageCount = if (pageAmountText.size > 0) pageAmountText.get(0).toInt() else 0
        val nextPageLinks = doc.selectFirst(".read-head").select("a").filter { it -> it.text() == "Next" }
        val chapterNames = doc.selectFirst(".sl-chap").select("option").filter { it -> it.hasAttr("selected") }
        val nextPageUrl = if (nextPageLinks.size > 0) nextPageLinks.get(0).attr("href") else "/"
        return PageResult(
            imageUrl = image.attr("src"),
            chapterName = if (chapterNames.size > 0) chapterNames.get(0).text() else "",
            pageAmount = pageAmount,
            pageCount = pageCount,
            nextPageUrl = if (nextPageUrl == "/") null else nextPageUrl
        )
    }
}