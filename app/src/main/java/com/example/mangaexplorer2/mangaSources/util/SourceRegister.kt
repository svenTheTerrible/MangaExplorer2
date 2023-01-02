package com.example.mangaexplorer2.mangaSources.util

import com.example.mangaexplorer2.mangaSources.MangaKakalot
import com.example.mangaexplorer2.mangaSources.MangaTown

class SourceRegister {
    companion object {
        private val sources: List<MangaSource> = listOf(MangaTown(), MangaKakalot())

        fun getSourceByString(sourceName: String): MangaSource {
            val mangaSource =
                sources.find { mangaSource -> mangaSource.sourceName.toString() == sourceName }
            return mangaSource ?: throw Error("Cannot find mangasource $sourceName")
        }

        fun getSourceByEnum(sourceName: MangaSourceName): MangaSource {
            return sources.find { it.sourceName == sourceName }
                ?: throw Error("Cannot find mangasource $sourceName")
        }
    }
}