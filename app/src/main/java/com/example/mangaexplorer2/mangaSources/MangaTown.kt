package com.example.mangaexplorer2.mangaSources

import android.os.Handler
import android.os.Looper
import com.example.mangaexplorer2.mangaSources.util.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.lang.Exception

class MangaTown() : MangaSource() {

    override val sourceName: MangaSourceName = MangaSourceName.MANGATOWN

    override val refererUrl = "https://www.mangatown.com/"

    override val mangaPageCache: MutableMap<String, MangaPageCache> = mutableMapOf()
    private val docCache: MutableMap<String, Document> = mutableMapOf();

    override fun getChapters(
        chapterMenuUrl: String,
        mangaName: String,
        callback: (chapters: List<ChapterResult>) -> Unit
    ) {
        val handler = Handler(Looper.getMainLooper())
        Thread {
            val chapters = getChaptersSync(chapterMenuUrl)
            handler.post {
                callback(chapters)
            }
        }.start()
    }

    private fun getChaptersSync(chapterMenuUrl: String): List<ChapterResult> {
        val doc = Jsoup.connect(chapterMenuUrl).referrer(refererUrl).timeout(jsoupTimeout).get()
        return doc.select(".chapter_list").select("a").map { chapterLink ->
            ChapterResult(
                name = chapterLink.text(),
                url = repairUrl(chapterLink.attr("href"))
            )
        }.reversed()
    }

    override fun getSearchResult(
        searchterm: String,
        callback: (searchResults: List<SearchResult>) -> Unit
    ) {
        val handler = Handler(Looper.getMainLooper())
        Thread {
            val doc = Jsoup.connect("https://www.mangatown.com/search.php?name=$searchterm").referrer(refererUrl).timeout(jsoupTimeout).get()
            val searchResults = doc.select(".manga_cover").map { resultItem ->
                SearchResult(
                    name = resultItem.attr("title"),
                    coverUrl = resultItem.getElementsByTag("img").attr("src"),
                    url = repairUrl(resultItem.attr("href"))
                )
            }
            handler.post {
                callback(searchResults)
            }
        }.start()
    }

    private fun repairUrlOptional(url: String?): String?{
        if(url == null){
            return null;
        }
        return repairUrl(url);
    }

    private fun getDoc(url: String): Document? {
        val test = repairUrl(url);
        return try {
            Jsoup.connect(test).referrer(refererUrl).timeout(jsoupTimeout).get()
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

    private fun getImageUrlsManwhaMode(doc: Document?): List<String>{
        return doc?.select(".image")?.map { element -> element.attr("src") } ?: emptyList<String>()
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

    private fun repairUrl(url: String): String {
        if(url.startsWith("https://www.mangatown.com")){
            return url;
        }
        if(url.startsWith("http://www.mangatown.com")){
            return url;
        }
        if(url.startsWith("https://mangatown.com")){
            return url.replace("https://", "https://www.")
        }
        if (url.startsWith("mangatown.com")) {
            return "https://www.$url"
        }
        if(url.startsWith("//")){
            return "http://$url";
        }
        return "https://www.mangatown.com$url";
    }

    private fun getPageNumberFromUrl(pageUrl: String): Int {
        val splitUrl = pageUrl.split("#")
        try {
            return Integer.parseInt(splitUrl.getOrNull(1) ?: "0")
        }catch (ex: NumberFormatException){
            return 0;
        }

    }

    private fun addPageNumberToUrl(pageUrl: String, pageNumber: Int): String{
        val splitUrl = pageUrl.split("#")
        val baseUrl = splitUrl[0];
        return "$baseUrl#$pageNumber";
    }

    private fun getPageResultManwhaMode(doc: Document?, imageUrls: List<String>, pageUrl: String): PageResult{
        val repairedImageUrls = imageUrls.map { repairUrlOptional(it) }
        val currentPage =getPageNumberFromUrl(pageUrl);
        val pageAmount = repairedImageUrls.size;
        val chapterOptions = getChapterOptions(doc)
        val selectedChapterIndex =
            chapterOptions?.indexOfFirst { element -> element.hasAttr("selected") }

        val chapterName =
            if (selectedChapterIndex != null) chapterOptions?.getOrNull(selectedChapterIndex)
                ?.text() else null

        val nextChapterOption =
            if (selectedChapterIndex != null) chapterOptions?.getOrNull(selectedChapterIndex + 1) else null
        val nextPageUrl = if(currentPage < repairedImageUrls.size -1) addPageNumberToUrl(pageUrl, currentPage +1) else repairUrlOptional(nextChapterOption?.attr("value"))
        return PageResult(
            imageUrl = repairedImageUrls.getOrNull(currentPage),
            chapterName = chapterName,
            pageAmount = pageAmount,
            pageCount = currentPage +1,
            nextPageUrl = repairUrlOptional(nextPageUrl)
        )
    }

    private fun getPageResultNormal(doc: Document?): PageResult{
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
            imageUrl = repairUrlOptional(imageUrl),
            chapterName = chapterName,
            pageAmount = pageAmount,
            pageCount = pageCount,
            nextPageUrl = repairUrlOptional(nextPage)
        )
    }

    private fun getCachedDoc(pageUrl: String): Document ? {
        val cachePageUrl = addPageNumberToUrl(pageUrl, 999)
        if(docCache.containsKey(cachePageUrl)){
            return docCache[cachePageUrl]
        }
        val newDoc =getDoc(pageUrl)
        if(newDoc != null){
            docCache.clear()
            docCache[cachePageUrl] = newDoc;
        }
        return newDoc
    }


    override fun getPageResultSync(pageUrl: String, chapterMenuUrl: String, mangaName: String): PageResult {
        val doc = getCachedDoc(pageUrl)
        val imageUrls = getImageUrlsManwhaMode(doc);
        return if (imageUrls.isNotEmpty()) getPageResultManwhaMode(doc, imageUrls, pageUrl) else getPageResultNormal(doc)
    }

}