package com.example.mangaexplorer2.MangaSources

import com.example.mangaexplorer2.MangaSources.util.*
import org.jsoup.Jsoup

class MangaTown() : MangaSource() {

    override val sourceName: MangaSourceName = MangaSourceName.MANGATOWN

    override fun getChapters(chapterMenuUrl: String, callback: (chapters: List<ChapterResult>) -> Unit): Unit {
        AsyncWrapper {
            val doc = Jsoup.connect(chapterMenuUrl).get()
            callback(
                doc.select(".chapter_list").select("a").map { chapterLink ->
                    ChapterResult(
                        name = chapterLink.text(),
                        url = repairUrl(chapterLink.attr("href"))
                    )
                }.reversed()
            )
        }.execute()
    }

    private fun repairUrl(url: String): String {
        return "https:" + url
    }

    override fun getSearchResult(searchterm: String, callback: (searchResults: List<SearchResult>) -> Unit): Unit {
        AsyncWrapper {
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

    private fun getChapterNameFromUrl(pageUrl: String): String? {
        val regex = """https:\/\/m\.mangatown\.com\/manga\/.*?\/(c.*?)(\/|${'$'})""".toRegex()
        val matchResult = regex.find(pageUrl)
        val groupValues = matchResult?.groupValues
        return if (groupValues != null && groupValues.size > 1) groupValues[1] else null
    }

    private fun toMobileUrl(url: String): String {
        val savePageUrl = url.replace("http://", "https://")
        val mobileUrl = savePageUrl.replace("https://www", "https://m")
        return if (mobileUrl.last().toString() == "/") mobileUrl.dropLast(1) else mobileUrl
    }

    override fun getPageResult(pageUrl: String, chapterMenuUrl: String, callback: (pageResult: PageResult) -> Unit) {
        AsyncWrapper {
            callback(getPageResultSync(pageUrl, chapterMenuUrl))
        }.execute()
    }

    override fun getPageResultSync(pageUrl: String, chapterMenuUrl: String): PageResult {
        val mobileUrl = toMobileUrl(pageUrl)
        val doc = Jsoup.connect(mobileUrl).get()
        val results = doc.select("#image").map { resultItem ->
            resultItem.attr("src")
        }
        val pageAmount = doc.select(".ch-select").select("option").size
        val selectedPageListElement =
            doc.select(".ch-select").select("option").find { element -> element.hasAttr("selected") }
        val pageCount = selectedPageListElement?.text()?.toInt()
        val nextPageElementA = doc.select("#viewer").select("a")
        val nextPage = if (nextPageElementA.size > 0) nextPageElementA[0].attr("href") else null
        return PageResult(
            imageUrl = if (results.size == 1) results[0] else null,
            chapterName = getChapterNameFromUrl(mobileUrl),
            pageAmount = pageAmount,
            pageCount = pageCount,
            nextPageUrl = if (nextPage == toMobileUrl(chapterMenuUrl)) null else nextPage
        )
    }
}