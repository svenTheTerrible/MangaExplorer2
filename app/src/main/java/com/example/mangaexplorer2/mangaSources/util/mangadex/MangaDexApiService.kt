package com.example.mangaexplorer2.mangaSources.util.mangadex;

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET;
import retrofit2.http.Path
import retrofit2.http.Query

public interface MangaDexApiService {

    @GET("/manga")
    suspend fun getUsers(
        @Query("title") searchTerm: String,
        @Query("availableTranslatedLanguage[]") language: String = "en",
        @Query("order[latestUploadedChapter]") order: String = "desc",
        @Query("limit") limit: Int = 20,
    ): MangaDexSearchResult

    @GET("/cover/{coverId}")
    suspend fun getSingleCover(
        @Path("coverId") coverId: String,
    ): MangaDexCoverImageResult

    @GET("/cover")
    suspend fun getMultipleCovers(
        @Query("ids[]") coverIds: List<String>,
        @Query("limit") limit: Int = 20
    ): MangaDexMultipleCoverImageResult


}


public class MangadexApi{

    public fun getApiService(): MangaDexApiService{
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.mangadex.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(MangaDexApiService::class.java)
        return api;
    }

}