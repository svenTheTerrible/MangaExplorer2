package com.example.mangaexplorer2.MangaSources

import com.example.mangaexplorer2.MangaSources.util.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.lang.Exception

class MangaKakalot() : MangaSource() {

    override val sourceName: MangaSourceName = MangaSourceName.MANGAKAKALOT

    override val refererUrl = "https://mangakakalot.com/"

    override val mangaPageCache: MutableMap<String, MangaPageCache> = mutableMapOf()


    override fun getChapters(
        chapterMenuUrl: String,
        mangaName: String,
        callback: (chapters: List<ChapterResult>) -> Unit
    ): Unit {
        AsyncWrapper {
            callback(getChaptersSync(chapterMenuUrl))
        }.execute()
    }

    private fun getChaptersSync(chapterMenuUrl: String): List<ChapterResult> {
        val doc = Jsoup.connect(chapterMenuUrl).referrer(refererUrl).get()
        return doc.select(".panel-story-chapter-list").select("li").map { chapterListItem ->
            val chapterLink = chapterListItem.select("a")
            ChapterResult(
                name = chapterLink.text(),
                url = chapterLink.attr("href")
            )
        }.reversed()
    }

    override fun getSearchResult(
        searchterm: String,
        callback: (searchResults: List<SearchResult>) -> Unit
    ): Unit {
        AsyncWrapper {
            val doc = Jsoup.connect("https://mangakakalot.com/search/story/$searchterm")
                .referrer(refererUrl).get()
            callback(
                doc.select(".story_item").map { resultItem ->
                    SearchResult(
                        name = resultItem.select(".story_name").select("a").text(),
                        coverUrl = resultItem.select("a").first().select("img").attr("src"),
                        url = resultItem.select(".story_name").select("a").attr("href")
                    )
                }
            )
        }.execute()
    }

    private fun getDoc(url: String): Document? {
        return try {
            Jsoup.connect(url).referrer(refererUrl).timeout(5000).get()
        } catch (
            e: Exception
        ) {
            null
        }
    }

    private fun removePageCountFromUrl(pageUrl: String): String {
        return pageUrl.replace("#page=\\d*".toRegex(), "")
    }

    private fun getCachedPageData(url: String): MangaPageCache {
        val cleanPageUrl = removePageCountFromUrl(url)
        if (mangaPageCache.containsKey(cleanPageUrl)) {
            return mangaPageCache.getValue(cleanPageUrl)
        }
        val doc = getDoc(cleanPageUrl)
        val imageUrls =
            doc?.select(".container-chapter-reader")?.select("img")?.map { it.attr("src") }
        val chapterOptions = doc?.select(".navi-change-chapter")?.select("option")
        val selectedChapterIndex = chapterOptions?.indexOfFirst { it.hasAttr("selected") }
        val selectedChapter =
            if (selectedChapterIndex != null) chapterOptions.getOrNull(selectedChapterIndex) else null
        val nextChapterIndex = selectedChapterIndex?.minus(1) ?: -1
        val nextChapterExists = nextChapterIndex >= 0
        val nextChapterOption =
            if (nextChapterExists) chapterOptions?.get(nextChapterIndex) else null
        val newCache = MangaPageCache(
            imageUrls,
            chapterName = selectedChapter?.text(),
            nextChapterUrl = generateNextChapterUrl(nextChapterOption, cleanPageUrl),
            pageAmount = imageUrls?.size ?: 0
        )
        mangaPageCache[cleanPageUrl] = newCache
        return newCache
    }

    private fun generateNextChapterUrl(nextChapterOption: Element?, url: String): String? {
        if (nextChapterOption == null) {
            return null
        }
        val nextPageChapterNumber = nextChapterOption.attr("data-c")
        return url.replace("chapter-.*".toRegex(), "chapter-$nextPageChapterNumber")
    }

    private fun getPageCountFromUrl(pageUrl: String): Int {
        val regex = "#page=(\\d*)".toRegex()
        val result = regex.find(pageUrl)
        val parsedPageNumberString = result?.groups?.get(1)?.value
        return parsedPageNumberString?.toInt() ?: 1
    }

    private fun generateNextPageUrl(
        pageUrl: String,
        pageCount: Int
    ): String {
        val cleanPageUrl = removePageCountFromUrl(pageUrl)
        val nextPageCount = pageCount + 1
        return "$cleanPageUrl#page=$nextPageCount"
    }

    override fun getPageResultSync(
        pageUrl: String,
        chapterMenuUrl: String,
        mangaName: String
    ): PageResult {
        val data = getCachedPageData(pageUrl)
        val pageCount = getPageCountFromUrl(pageUrl)
        val nextImageUrl = data?.images?.getOrNull(pageCount)
        val nextPageUrl = if(nextImageUrl != null) generateNextPageUrl(pageUrl, pageCount) else data.nextChapterUrl


        return PageResult(
            imageUrl = data?.images?.get(pageCount -1),
            chapterName = data.chapterName,
            pageAmount = data.pageAmount,
            pageCount = pageCount,
            nextPageUrl = nextPageUrl
        )
    }

}