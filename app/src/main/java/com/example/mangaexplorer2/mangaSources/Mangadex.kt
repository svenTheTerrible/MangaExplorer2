package com.example.mangaexplorer2.mangaSources

import android.os.Handler
import android.os.Looper
import com.example.mangaexplorer2.mangaSources.util.ChapterResult
import com.example.mangaexplorer2.mangaSources.util.MangaPageCache
import com.example.mangaexplorer2.mangaSources.util.MangaSource
import com.example.mangaexplorer2.mangaSources.util.MangaSourceName
import com.example.mangaexplorer2.mangaSources.util.PageResult
import com.example.mangaexplorer2.mangaSources.util.SearchResult
import com.example.mangaexplorer2.mangaSources.util.mangadex.MangaDexApiService
import com.example.mangaexplorer2.mangaSources.util.mangadex.MangaDexChapterResultData
import com.example.mangaexplorer2.mangaSources.util.mangadex.MangaDexChapterResultDataAttributes
import com.example.mangaexplorer2.mangaSources.util.mangadex.MangaDexMultipleCoverImageResult
import com.example.mangaexplorer2.mangaSources.util.mangadex.MangaDexSearchResultItem
import com.example.mangaexplorer2.mangaSources.util.mangadex.MangaDexSearchResultItemRelationShip
import com.example.mangaexplorer2.mangaSources.util.mangadex.MangadexApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class Mangadex(): MangaSource() {

    override val sourceName: MangaSourceName = MangaSourceName.MANGADEX
    override val refererUrl: String = "https://mangadex.org/"
    override val mangaPageCache: MutableMap<String, MangaPageCache> = mutableMapOf()

    override fun getChapters(
        chapterMenuUrl: String,
        mangaName: String,
        callback: (List<ChapterResult>) -> Unit
    ) {
        val handler = Handler(Looper.getMainLooper())
        CoroutineScope(Dispatchers.IO).launch {
            val api = MangadexApi().getApiService()
            val chapterList = loadCompleteChapterList(api, chapterMenuUrl)
            val chapterResult = chapterList.map { ChapterResult(makeChapterName(it.attributes), it.id) }
            handler.post {
                callback(
                    chapterResult
                )
            }
        }
    }

    private suspend fun loadCompleteChapterList(api: MangaDexApiService, mangaId: String): List<MangaDexChapterResultData>{
        val fullList = mutableListOf<MangaDexChapterResultData>()
        var chaptersFullyLoaded = false
        var offset = 0
        while(!chaptersFullyLoaded){
            val apiResult = api.getChapters(mangaId, offset)
            fullList.addAll(apiResult.data)
            chaptersFullyLoaded = (apiResult.offset + apiResult.limit) >= apiResult.total
            offset += 100
        }
        return fullList;
    }

    private fun makeChapterName(chapterAttributes: MangaDexChapterResultDataAttributes?): String {
        if(chapterAttributes == null){
            return ""
        }
        return listOfNotNull(
            chapterAttributes.volume,
            chapterAttributes.chapter,
            chapterAttributes.title
        ).joinToString(" ")
    }



    override fun getSearchResult(
        searchterm: String,
        callback: (List<SearchResult>) -> Unit
    ) {

        val handler = Handler(Looper.getMainLooper())
        CoroutineScope(Dispatchers.IO).launch {
            val api = MangadexApi().getApiService()
            val mangas = api.getUsers(searchterm)
            val coverIds =
                mangas.data.mapNotNull { getCoverArtsFromRelationships(it.relationships) }
            val covers = api.getMultipleCovers(coverIds)

            val searchResults = mangas.data.map { singleManga ->
                SearchResult(getTitleFromSearchResult(singleManga),
                    singleManga.id,
                    makeCoverUrl(covers,  singleManga.id)) }
            handler.post {
                callback(
                    searchResults
                )
            }
        }
    }

    private fun makeCoverUrl(allCovers: MangaDexMultipleCoverImageResult, mangaId: String): String{
        val fileName = allCovers.data.find { coverRelationShipBelongsToMangaId(it.relationships, mangaId)  }?.attributes?.fileName
        if(fileName == null){
            return ""
        }
        return "https://uploads.mangadex.org/covers/$mangaId/$fileName";
    }

    private fun coverRelationShipBelongsToMangaId(relationships: List<MangaDexSearchResultItemRelationShip>, mangaId: String): Boolean{
        return relationships.any { it.type == "manga" && it.id== mangaId }
    }

    private fun getCoverArtsFromRelationships(list: List<MangaDexSearchResultItemRelationShip>): String?{
        return list.find { it.type == "cover_art" }?.id
    }

    private fun getTitleFromSearchResult(item: MangaDexSearchResultItem): String {
       return   item.attributes.title.getOrElse("en", {""})
    }

    override fun getPageResultSync(
        pageUrl: String,
        chapterMenuUrl: String,
        mangaName: String
    ): PageResult {
        val splitPageUrl = pageUrl.split("/")
        val cleanPageUrl = splitPageUrl.first()
        val currentPage = if(splitPageUrl.size == 2) splitPageUrl.last() else "1"
        val currentPageNumber =  currentPage.toInt()
        val nextPageNumber = currentPageNumber +1

        val cache = mangaPageCache.get(cleanPageUrl)

        if(cache != null){
            return makePageResultFromCache(cache, currentPageNumber, nextPageNumber, cleanPageUrl)
        }

        val api = MangadexApi().getApiService()
        val result = runBlocking { api.getPageDataForChapter(cleanPageUrl) }

        //todo call https://api.mangadex.org/at-home/server/a20b549f-8332-42aa-8584-ef91f4edf43f
        // weird hash is pageUrl at end
        //todo https://cmdxd98sb0x3yprd.mangadex.network/data/01197a88e280da31ccccde46b5dce08e/1-e3421734aa975fc4112c015c8433dfd1cf62017d99a93967aa1605e928b379ad.jpg

        val chapterList = runBlocking { loadCompleteChapterList(api, chapterMenuUrl) }

        val currentChapterIndex = chapterList.indexOfFirst { it.id == cleanPageUrl }

        val currentChapterName = makeChapterName(chapterList.getOrNull(currentChapterIndex)?.attributes)

        val newCache = MangaPageCache(
            result.chapter.data.map { result.baseUrl + "/data/" + result.chapter.hash + "/"  + it },
            currentChapterName,
            result.chapter.data.size,
            chapterList.getOrNull(currentChapterIndex)?.id
        )

        mangaPageCache.put(cleanPageUrl, newCache)

        return makePageResultFromCache(newCache, currentPageNumber, nextPageNumber, cleanPageUrl);
    }

    private fun makePageResultFromCache(cache: MangaPageCache, currentPageNumber: Int, nextPageNumber: Int, cleanPageUrl: String): PageResult{
        val savePageAmount = cache.pageAmount?:0
        return PageResult(
            cache.images?.get(currentPageNumber -1),
            currentPageNumber,
            cache.pageAmount,
            cache.chapterName,
            if(savePageAmount < nextPageNumber) cache.nextChapterUrl else "$cleanPageUrl/$nextPageNumber"
        )
    }

    override fun clearAllCaches(): MangaSource {
        TODO("Not yet implemented")
    }
}