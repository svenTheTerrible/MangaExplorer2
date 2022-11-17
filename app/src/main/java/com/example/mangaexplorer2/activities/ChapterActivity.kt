package com.example.mangaexplorer2.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.mangaexplorer2.adapters.ChapterResultListItemAdapter
import com.example.mangaexplorer2.GlideApp
import com.example.mangaexplorer2.mangaSources.util.ChapterResult
import com.example.mangaexplorer2.mangaSources.util.MangaSource
import com.example.mangaexplorer2.mangaSources.util.SearchResult
import com.example.mangaexplorer2.databinding.ActivityChapterBinding
import com.example.mangaexplorer2.databinding.ContentChapterBinding

class ChapterActivity : AppCompatActivity() {
    private lateinit var activityBinding: ActivityChapterBinding;
    private lateinit var contentBinding: ContentChapterBinding;
    private lateinit var mangaSource: MangaSource;
    private lateinit var searchResult: SearchResult;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityBinding = ActivityChapterBinding.inflate(layoutInflater)
        setContentView(activityBinding.root)
        setSupportActionBar(activityBinding.chapterToolbar)
        unpackExtras()
        activityBinding.chapterToolbar.title = searchResult.name

        val glideUrl = GlideUrl(
            searchResult.coverUrl,
            LazyHeaders.Builder().addHeader("Referer", mangaSource.refererUrl).build()
        )

        GlideApp.with(this)
            .load(glideUrl).diskCacheStrategy(DiskCacheStrategy.NONE)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(activityBinding.appBarMangaCover)

        loadChapters()
    }

    private fun loadChapters(): Unit {
        updateChapterResults(emptyList(), true)
        mangaSource.getChapters(searchResult.url, searchResult.name) { chapters ->
            updateChapterResults(chapters, false)
        }
    }

    private fun updateChapterResults(chapters: List<ChapterResult>, isLoading: Boolean): Unit {
        contentBinding.chapterProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        contentBinding.chapterRecyclerView.layoutManager =
            LinearLayoutManager(
                this,
                RecyclerView.VERTICAL,
                false
            )
        contentBinding.chapterRecyclerView.adapter = ChapterResultListItemAdapter(chapters, ::onClickChapterResult)
    }

    private fun onClickChapterResult(chapterResult: ChapterResult): Unit {
        val intent = Intent(this, PageReaderActivity::class.java)
        intent.putExtra("mangaSource", mangaSource)
        intent.putExtra("searchResult", searchResult)
        intent.putExtra("chapterResult", chapterResult)
        startActivity(intent)

    }


    private fun unpackExtras(): Unit {
        val extras = intent.extras ?: throw Error("extras is missing")
        mangaSource =
            (extras.getSerializable("mangaSource")
                ?: throw Error("sourceName is missing")) as? MangaSource
                ?: throw Error("Serializable is no MangaSource")
        searchResult = (extras.getSerializable("searchResult")
            ?: throw Error("searchResult is missing")) as? SearchResult
            ?: throw Error("Serializable is no SearchResult")
    }

}
