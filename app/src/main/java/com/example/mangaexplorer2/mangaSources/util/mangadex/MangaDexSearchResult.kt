package com.example.mangaexplorer2.mangaSources.util.mangadex;

data class MangaDexSearchResult(
    val result: String,
    val response: String,
    val data: List<MangaDexSearchResultItem>,
    val limit: Int,
    val offset: Int,
    val total: Int
)

data class MangaDexSearchResultItem(
    val id: String,
    val type: String,
    val attributes: MangaDexSearchResultItemAttribute,
    val relationships: List<MangaDexSearchResultItemRelationShip>
)

data class MangaDexSearchResultItemAttribute(
    val title: Map<String, String>,
    val altTitles: List<Map<String, String>>
)

data class MangaDexSearchResultItemRelationShip(
    val type: String,
    val id: String
)

data class MangaDexCoverImageResult(
    val result: String,
    val response: String,
    val data: MangaDexCoverImageData
)


data class MangaDexCoverImageData(
    val id: String,
    val type: String,
    val attributes: MangaDexCoverImageDataAttributes,
    val relationships: List<MangaDexSearchResultItemRelationShip>
)

data class MangaDexCoverImageDataAttributes(
    val fileName: String
)

data class MangaDexMultipleCoverImageResult(
    val result: String,
    val response: String,
    val data: List<MangaDexCoverImageData>
)

data class MangaDexChapterResult(
    val result: String,
    val response: String,
    val data: List<MangaDexChapterResultData>,
    val limit: Int,
    val offset: Int,
    val total: Int
)

data class MangaDexChapterResultData(
    val id: String,
    val type: String,
    val attributes: MangaDexChapterResultDataAttributes
)

data class MangaDexChapterResultDataAttributes(
    val volume: String,
    val chapter: String,
    val title: String,
    val pages: Int
)