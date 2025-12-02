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
            val doc = Jsoup.connect("https://www.mangatown.com/search.php?name=$searchterm")
                .cookie("cookie", "DM5_MACHINEKEY=cc1c76e0-bb0e-4cc3-af74-5a6c73cf5239; __utmc=1; __utmz=1.1715794173.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); _ga=GA1.1.258659861.1715794180; read_tsukkomi=; image_time_cookie=519669|638514200507675583|1,194087|638514209951745385|0; dm5imgpage=519669|6:0,194087|1:0; readhistoryitem=History=28913,638514200507815541,519669,6,0,0,0,1|12005,638514209951855350,194087,1,0,0,0,1&ViewType=0; readhistory_time=12005-194087-1; imageload=519669%7C13%2C194087%7C2; _ga_RRD7Q6C508=GS1.1.1715794179.1.1.1715796494.0.0.0; webstickynode=035d7b9dfbcd7439e99dd492ed943c34; __utma=1.1912077701.1715794173.1715794173.1715797060.2; __utmt=1; __utmb=1.1.10.1715797060")
                .header("Pragma", "no-cache")
                .header("Sec-Ch-Ua", "\"Chromium\";v=\"123\", \"Not:A-Brand\";v=\"8\"")
                .header("Sec-Ch-Ua-Mobile", "?0")
                .header("Sec-Ch-Ua-Platform", "Linux\"")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("Accept-Encoding", "gzip, deflate, br, zstd")
                .header("Accept-Language", "en-US,en;q=0.9,de;q=0.8")
                .header("Cache-Control", "no-cache")
                .header("Accept-Encoding", "gzip")
                .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                .referrer(refererUrl)
                .timeout(jsoupTimeout)
                .get()
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

    override fun clearAllCaches(): MangaSource {
        this.mangaPageCache.clear()
        this.docCache.clear()
        return this
    }
}