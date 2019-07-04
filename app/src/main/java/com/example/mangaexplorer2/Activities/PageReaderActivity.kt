package com.example.mangaexplorer2.Activities

import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.mangaexplorer2.GlideApp
import com.example.mangaexplorer2.MangaSources.ChapterResult
import com.example.mangaexplorer2.MangaSources.MangaSource
import com.example.mangaexplorer2.MangaSources.PageResult
import com.example.mangaexplorer2.MangaSources.SearchResult
import com.example.mangaexplorer2.R
import kotlinx.android.synthetic.main.activity_page_reader.*
import kotlinx.android.synthetic.main.nav_header_main.*

class PageReaderActivity : AppCompatActivity() {
    private var mangaSource: MangaSource? = null
    private var searchResult: SearchResult? = null
    private var chapterResult: ChapterResult? = null


    private var currentPageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page_reader)
        unpackExtras()
        currentPageUrl = chapterResult?.url
        supportActionBar?.hide()

        loadCurrentPageImage()
    }

    private fun unpackExtras(): Unit{
        val extras = intent.extras?: throw Error("extras is missing")
        mangaSource =
            (extras.getSerializable("mangaSource") ?: throw Error("sourceName is missing")) as? MangaSource ?: throw Error("Serializable is no MangaSource")
        searchResult = (extras.getSerializable("searchResult")?: throw Error("searchResult is missing")) as? SearchResult ?: throw Error("Serializable is no SearchResult")
        chapterResult = (extras.getSerializable("chapterResult")?: throw Error("chapterResult is missing")) as? ChapterResult ?: throw Error("Serializable is no ChapterResult")
    }

    private fun loadCurrentPageImage(){
        val pageUrl = currentPageUrl
        if(pageUrl != null){
            mangaSource?.getPageResult(pageUrl) { pageResult->
                updateImageView(pageResult)
            }
        }
    }

    private fun updateImageView(pageResult: PageResult):Unit{
        this@PageReaderActivity.runOnUiThread{

            //todo resolve any other disc cache strategy except none


            chapterNameTextView.text = pageResult.chapterName
            pageCountTextView.text = pageResult.pageCount?.toString() + "/" + pageResult.pageAmount?.toString()

         GlideApp.with(this)
             .load(Uri.parse(pageResult.imageUrl)).diskCacheStrategy(DiskCacheStrategy.NONE)
             .into(pageReaderImageView)
        }
    }

}
