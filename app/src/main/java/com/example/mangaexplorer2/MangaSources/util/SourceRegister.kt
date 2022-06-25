package com.example.mangaexplorer2.MangaSources.util

import com.example.mangaexplorer2.MangaSources.MangaKakalot
import com.example.mangaexplorer2.MangaSources.MangaTown

class SourceRegister {
    private val sources: List<MangaSource> = listOf(MangaTown(), MangaKakalot())

    fun getSource(sourceName: String): MangaSource {
        val mangaSource = sources.find { mangaSource -> mangaSource.sourceName.toString() == sourceName }
        return mangaSource ?: throw Error("Cannot find mangasource $sourceName")
    }
}