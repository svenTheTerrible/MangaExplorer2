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

    private fun makeChapterName(chapterAttributes: MangaDexChapterResultDataAttributes): String {
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
        TODO("Not yet implemented")
    }

    override fun clearAllCaches(): MangaSource {
        TODO("Not yet implemented")
    }
}