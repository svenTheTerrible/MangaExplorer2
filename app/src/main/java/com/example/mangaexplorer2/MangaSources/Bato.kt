package com.example.mangaexplorer2.MangaSources

import com.example.mangaexplorer2.MangaSources.util.*
import org.jsoup.Jsoup

class ImageInfo(val pageNumber: Int, val imageUrl: String)

class Bato() : MangaSource() {

    override val sourceName: MangaSourceName = MangaSourceName.BATO

    override fun getSearchResult(searchterm: String, callback: (searchResults: List<SearchResult>) -> Unit): Unit {
        AsyncWrapper {
            val doc = Jsoup.connect("https://bato.to/search?q=$searchterm").get()
            callback(
                doc.select("#series-list").select(".item").map { resultItem ->
                    val coverLink = resultItem.select(".item-cover")
                    SearchResult(
                        name = resultItem.select(".item-title").text(),
                        coverUrl = coverLink.select("img").attr("src").replace("//", "https://"),
                        url = toBatoUrl(coverLink.attr("href"))
                    )
                }
            )
        }.execute()
    }

    override fun getChapters(chapterMenuUrl: String, callback: (chapters: List<ChapterResult>) -> Unit): Unit {
        AsyncWrapper {
            val doc = Jsoup.connect(chapterMenuUrl).get()
            callback(
                doc.select(".main").select(".item").map { chapterItem ->
                    ChapterResult(
                        name = chapterItem.select(".chapt").select("b").text(),
                        url = toBatoUrl(chapterItem.select(".chapt").attr("href"))
                    )
                }.reversed()
            )
        }.execute()
    }

    private fun toBatoUrl(incompleteUrl: String): String {
        return "https://bato.to$incompleteUrl"
    }

    override fun getPageResultSync(pageUrl: String, chapterMenuUrl: String): PageResult {
        val doc = Jsoup.connect(pageUrl).timeout(5000).get()
        val docString = doc.html()
        val jsonMatch = """var images = (\{.*?\})""".toRegex().find(docString, 0)
        val json =
            (if (jsonMatch != null && jsonMatch.groupValues.size > 1) jsonMatch.groupValues[1] else throw Error("Could not parse pages from javascript"))
        val pageInfos = json.split(",").map { snippet ->
            val match = """"(\d*)":"(.*?)"""".toRegex().find(snippet)
            if (match != null && match.groupValues.size > 2) {
                ImageInfo(match.groupValues[1].toInt(), match.groupValues[2])
            } else {
                null
            }
        }.filter { imageInfo -> imageInfo != null } as List<ImageInfo>

        val pageNumber = getPageFromUrl(pageUrl)

        val matchingPageInfo = pageInfos.find { pageInfo -> pageInfo.pageNumber == pageNumber }
            ?: throw Error("Available pages did not match current pageNumber")

        val nextPageAvailable = pageInfos.find { pageInfo -> pageInfo.pageNumber == pageNumber + 1 }

        val nextChapterUrl = toBatoUrl(doc.select(".nav-next").select("a").attr("href"))

        val nextPageUrl = if (nextPageAvailable != null) makePageUrl(pageUrl, pageNumber + 1) else nextChapterUrl

        val chapterName = doc.select(".nav-chap").select("option").find { option -> option.hasAttr("selected") }?.text()


        return PageResult(
            imageUrl = matchingPageInfo.imageUrl,
            chapterName = chapterName ?: "",
            pageAmount = pageInfos.size,
            pageCount = pageNumber,
            nextPageUrl = if (nextPageUrl == chapterMenuUrl) null else nextPageUrl
        )

    }

    private fun makePageUrl(pageUrl: String, pageNumber: Int): String {
        val cleanPageUrl = """(https:\/\/bato\.to\/chapter\/\d*)""".toRegex().find(pageUrl)
        if (cleanPageUrl != null && cleanPageUrl.groupValues.size > 1) {
            return cleanPageUrl.groupValues[1] + "/" + pageNumber
        }
        throw Error("Could not parse clean url for next page")
    }

    private fun getPageFromUrl(pageUrl: String): Int {
        val pageNumberMatch = """https:\/\/bato\.to\/chapter\/\d*\/(\d*)""".toRegex().find(pageUrl)
        if (pageNumberMatch != null && pageNumberMatch.groupValues.size > 1) {
            return pageNumberMatch.groupValues[1].toInt()
        }
        return 1
    }

}