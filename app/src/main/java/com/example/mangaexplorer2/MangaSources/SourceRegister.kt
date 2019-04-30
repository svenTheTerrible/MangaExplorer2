package com.example.mangaexplorer2.MangaSources

class SourceRegister {
    private val sources: List<MangaSource> = listOf(MangaTown())

    fun getSource(sourceName: String): MangaSource {
        val mangaSource = sources.find { mangaSource -> mangaSource.sourceName.toString() == sourceName }
        return mangaSource ?: throw Error("Cannot find mangasource $sourceName")
    }
}