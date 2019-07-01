package com.example.mangaexplorer2.Activities

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import com.example.mangaexplorer2.MangaSources.MangaSource
import com.example.mangaexplorer2.MangaSources.SearchResult
import com.example.mangaexplorer2.R
import kotlinx.android.synthetic.main.activity_chapter.*

class ChapterActivity : AppCompatActivity() {
    private var mangaSource: MangaSource? = null;
    private var searchResult: SearchResult? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chapter)
        setSupportActionBar(toolbar)
        unpackExtras()
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }


        //chapterRecyclerView.set

    }


    private fun unpackExtras(): Unit{
        val extras = intent.extras?: throw Error("extras is missing")
        mangaSource =
            (extras.getSerializable("mangaSource") ?: throw Error("sourceName is missing")) as? MangaSource ?: throw Error("Serializable is no MangaSource")
        searchResult = (extras.getSerializable("searchResult")?: throw Error("searchResult is missing")) as? SearchResult ?: throw Error("Serializable is no SearchResult")
    }

}
