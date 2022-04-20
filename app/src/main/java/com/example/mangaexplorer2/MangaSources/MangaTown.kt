package com.example.mangaexplorer2.MangaSources

import com.example.mangaexplorer2.MangaSources.util.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.lang.Exception

class MangaTown() : MangaSource() {

    override val sourceName: MangaSourceName = MangaSourceName.MANGATOWN

    override fun getChapters(
        chapterMenuUrl: String,
        callback: (chapters: List<ChapterResult>) -> Unit
    ): Unit {
        AsyncWrapper {
            callback(getChaptersSync(chapterMenuUrl))
        }.execute()
    }

    private fun getChaptersSync(chapterMenuUrl: String): List<ChapterResult> {
        val doc = Jsoup.connect(chapterMenuUrl).referrer("https://www.mangatown.com/").get()
        return doc.select(".chapter_list").select("a").map { chapterLink ->
            ChapterResult(
                name = chapterLink.text(),
                url = repairUrl(chapterLink.attr("href"))
            )
        }.reversed()
    }

    private fun repairUrl(url: String): String {
        if (url.contains("mangatown.com")) {
            return "https:" + url
        }
        return "https://www.mangatown.com" + url;
    }

    override fun getSearchResult(
        searchterm: String,
        callback: (searchResults: List<SearchResult>) -> Unit
    ): Unit {
        AsyncWrapper {
            val doc = Jsoup.connect("https://www.mangatown.com/search.php?name=$searchterm").referrer("https://www.mangatown.com/").get()
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

    private fun repairImageUrl(url: String?): String? {
        if (url == null) {
            return null
        }
        val test = url.subSequence(0, 2)
        if (test == "//") {
            return "http://$url";
        }
        return url
    }

    private fun getDoc(url: String): Document? {
        return try {
            Jsoup.connect(url).referrer("https://www.mangatown.com/").timeout(5000).get()
        } catch (
            e: Exception
        ) {
            null
        }
    }

    private fun getImageUrl(doc: Document?): String? {
        return try {
            val result = doc?.select("#image")?.attr("src")
            if(result == null || result.isEmpty()) null else result
        } catch (e: Exception) {
            null
        }
    }

    private fun getAvailablePageOptions(doc: Document?): List<Element>? {
        return try {
            doc?.select(".manga_read_footer")?.select(".page_select")?.select("option")?.toList()?.filter { option ->
                val text = option.text()
                try {
                    text.toInt()
                    true
                }catch (e: NumberFormatException){
                    false
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getChapterOptions(doc: Document?): List<Element>? {
        return try {
            doc?.select("#bottom_chapter_list")?.select("option")?.toList()
        } catch (e: Exception) {
            null
        }
    }

    private fun repairNextPageUrl(url: String?): String? {

        if (url == null) {
            return null
        }

        if (url.contains("http")) {
            return url
        }
        return "https://mangatown.com" + url
    }

    override fun getPageResultSync(pageUrl: String, chapterMenuUrl: String): PageResult {
        val doc = getDoc(pageUrl)
        val imageUrl = getImageUrl(doc)
        val availablePageOptions = getAvailablePageOptions(doc)
        val pageAmount = availablePageOptions?.size ?: 0

        val selectedPageIndex =
            availablePageOptions?.indexOfLast { element -> element.hasAttr("selected") }
        val pageCount =
            if (selectedPageIndex == -1 || selectedPageIndex == null) null else selectedPageIndex + 1
        val chapterOptions = getChapterOptions(doc)
        val selectedChapterIndex =
            chapterOptions?.indexOfFirst { element -> element.hasAttr("selected") }

        val chapterName =
            if (selectedChapterIndex != null) chapterOptions?.getOrNull(selectedChapterIndex)
                ?.text() else null
        val nextChapterOption =
            if (selectedChapterIndex != null) chapterOptions?.getOrNull(selectedChapterIndex + 1) else null
        val nextPageOption =
            if (selectedPageIndex != null) availablePageOptions?.getOrNull(selectedPageIndex + 1) else null
        val nextPage =
            if (nextPageOption != null) nextPageOption.attr("value") else nextChapterOption?.attr("value")
        return PageResult(
            imageUrl = repairImageUrl(imageUrl),
            chapterName = chapterName,
            pageAmount = pageAmount,
            pageCount = pageCount,
            nextPageUrl = repairNextPageUrl(nextPage)
        )
    }

}