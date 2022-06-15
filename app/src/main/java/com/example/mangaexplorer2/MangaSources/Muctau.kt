package com.example.mangaexplorer2.MangaSources

import com.example.mangaexplorer2.MangaSources.util.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.lang.Exception

class Muctau() : MangaSource() {

    override val sourceName: MangaSourceName = MangaSourceName.MUCTAU

    override val refererUrl = "https://bibimanga.com"

    override val mangaPageCache: MutableMap<String, MangaPageCache> = mutableMapOf()

    override fun getChapters(
        chapterMenuUrl: String,
        mangaName: String,
        callback: (chapters: List<ChapterResult>) -> Unit
    ): Unit {
        AsyncWrapper {
            callback(getChaptersSync(chapterMenuUrl, mangaName))
        }.execute()
    }


    private fun getDocFromVariableUrl(staticUrl: String, mangaName: String, allowChapterMenu: Boolean): Pair<String, Document> {
        val bibiUrl = updateToBibiMangaUrl(staticUrl)
        val firstDoc = Jsoup.connect(bibiUrl).referrer(refererUrl).get()
        val isLandingPage = firstDoc.select("meta")
            .any { it.attr("property") == "og:title" && it.attr("content") == "Read Manga Online for Free!" }
        val isChapterPage = !allowChapterMenu && firstDoc.select(".version-chap").select("li").isNotEmpty()
        if (!isLandingPage && !isChapterPage) {
            return Pair(bibiUrl, firstDoc)
        }
        val searchResults = getSearchResultSync(mangaName)
        if (searchResults.isEmpty()) {
            return Pair(bibiUrl, firstDoc)
        }
        return getDocFromVariableUrl(
            updateVariablePartInUrl(
                getVariableChapterUrlPart(bibiUrl),
                getVariableChapterUrlPart(searchResults[0].url),
                bibiUrl
            ), mangaName,
            allowChapterMenu
        )
    }

    private fun getChaptersSync(chapterMenuUrl: String, mangaName: String): List<ChapterResult> {
        val (_, doc) = getDocFromVariableUrl(chapterMenuUrl, mangaName, true)
        return doc.select(".version-chap").select("li").map { chapterItem ->
            val chapterLink = chapterItem.select("a").first()
            ChapterResult(
                name = chapterLink.text(),
                url = chapterLink.attr("href")
            )
        }.reversed()
    }

    private fun getSearchResultSync(searchterm: String): List<SearchResult> {
        val doc =
            Jsoup.connect("https://muctau.com/?s=$searchterm&post_type=wp-manga&post_type=wp-manga")
                .referrer(refererUrl).get()
        return doc.select(".c-tabs-item__content").map { searchItem ->
            SearchResult(
                name = searchItem.select(".post-title").select("a").text(),
                coverUrl = searchItem.select(".c-image-hover").select("img")
                    .attr("data-src"),
                url = searchItem.select(".post-title").select("a").attr("href")
            )
        }
    }

    override fun getSearchResult(
        searchterm: String,
        callback: (searchResults: List<SearchResult>) -> Unit
    ): Unit {
        AsyncWrapper {
            callback(getSearchResultSync(searchterm))
        }.execute()
    }

    private fun getVariableChapterUrlPart(url: String): String {
        val regex = "\\/manga\\/(\\D\\D\\D\\D\\D\\D)-".toRegex()
        val result = regex.find(url)
        val variablePart = result?.groups?.get(1)?.value
        return variablePart ?: ""
    }

    private fun updateToBibiMangaUrl(mangaUrl: String): String{
        return mangaUrl.replace("https://muctau.com", "https://bibimanga.com")
    }

    private fun updateVariablePartInUrl(
        variableOld: String,
        variableNew: String,
        url: String
    ): String {
        return url.replace(variableOld, variableNew)
    }

    private fun requestDoc(url: String): Document {
        return Jsoup.connect(url).referrer(refererUrl).timeout(5000).get()
    }

    private fun cleanStringFromTabsAndReturns(value: String): String {
        return value.replace("\n", "").replace("\t", "")
    }

    private fun getImageUrls(doc: Document?): List<String>? {
        return try {
            val result = doc?.select(".reading-content")?.select("div")
                ?.map { pageItem -> pageItem.select("img").attr("data-src") }
            if (result == null || result.isEmpty()) null else result.filter { it.isNotEmpty() }
                .map { cleanStringFromTabsAndReturns(it) }
                //index 0 is removed, because its always a duplicate
                .filterIndexed { index, _ -> index != 0 }
        } catch (e: Exception) {
            null
        }
    }

    private fun getChapterOptions(doc: Document?): List<Element>? {
        return try {
            doc?.select(".single-chapter-select")?.select("option")?.toList()
        } catch (e: Exception) {
            null
        }
    }

    private fun getCachedParsedPageData(pageUrl: String, doc: Document?): MangaPageCache {
        val pageUrlWithoutPageNumber = removePageCountFromUrl(pageUrl)
        if (mangaPageCache.containsKey(pageUrlWithoutPageNumber)) {
            return mangaPageCache[pageUrlWithoutPageNumber]!!
        }
        val imageUrls = getImageUrls(doc)
        val chapterOptions = getChapterOptions(doc)
        val selectedChapterIndex =
            chapterOptions?.indexOfFirst { it.attr("selected") == "selected" }
        val selectedChapterName =
            if (selectedChapterIndex != null) chapterOptions[selectedChapterIndex].text() else null
        val nextChapterUrl =
            if (selectedChapterIndex != null) chapterOptions[selectedChapterIndex - 1].attr("data-redirect") else null
        val data = MangaPageCache(
            imageUrls,
            selectedChapterName,
            imageUrls?.size ?: 0,
            nextChapterUrl
        )
        mangaPageCache[pageUrlWithoutPageNumber] = data
        return data
    }

    private fun removePageCountFromUrl(pageUrl: String): String {
        return pageUrl.replace("#page=\\d*".toRegex(), "")
    }

    private fun getPageCountFromUrl(pageUrl: String): Int {
        val regex = "#page=(\\d*)".toRegex()
        val result = regex.find(pageUrl)
        val parsedPageNumberString = result?.groups?.get(1)?.value
        return parsedPageNumberString?.toInt() ?: 1
    }

    private fun generateNextPageUrl(
        pageUrl: String,
        pageCount: Int,
        parsedResult: MangaPageCache
    ): String? {
        val images = parsedResult.images ?: return null
        if (pageCount >= images.size) {
            return parsedResult.nextChapterUrl
        }
        val cleanPageUrl = removePageCountFromUrl(pageUrl)
        val nextPageCount = pageCount + 1
        return "$cleanPageUrl#page=$nextPageCount"
    }

    override fun getPageResultSync(pageUrl: String, chapterMenuUrl: String, mangaName: String): PageResult {
        val (url, doc) = getDocFromVariableUrl(pageUrl, mangaName, false)
        val parseResult = getCachedParsedPageData(url, doc)
        val pageCount = getPageCountFromUrl(pageUrl)

        return PageResult(
            imageUrl = parseResult.images?.get(pageCount - 1),
            chapterName = parseResult.chapterName,
            pageAmount = parseResult.pageAmount,
            pageCount = pageCount,
            nextPageUrl = generateNextPageUrl(url, pageCount, parseResult)
        )
    }

}