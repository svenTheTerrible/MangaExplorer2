package com.example.mangaexplorer2.MangaSources.util

import com.example.mangaexplorer2.MangaSources.MangaTown
import com.example.mangaexplorer2.MangaSources.Muctau
import com.example.mangaexplorer2.MangaSources.TenManga

class SourceRegister {
    private val sources: List<MangaSource> = listOf(MangaTown(), TenManga(), Muctau())

    fun getSource(sourceName: String): MangaSource {
        val mangaSource = sources.find { mangaSource -> mangaSource.sourceName.toString() == sourceName }
        return mangaSource ?: throw Error("Cannot find mangasource $sourceName")
    }
}