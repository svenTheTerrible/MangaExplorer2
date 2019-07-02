package com.example.mangaexplorer2.Activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.widget.LinearLayout
import com.example.mangaexplorer2.Adapters.ChapterResultListItemAdapter
import com.example.mangaexplorer2.MangaSources.ChapterResult
import com.example.mangaexplorer2.MangaSources.MangaSource
import com.example.mangaexplorer2.MangaSources.SearchResult
import com.example.mangaexplorer2.R
import kotlinx.android.synthetic.main.activity_chapter.*
import kotlinx.android.synthetic.main.content_chapter.*

class ChapterActivity : AppCompatActivity() {
    private var mangaSource: MangaSource? = null;
    private var searchResult: SearchResult? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chapter)
        setSupportActionBar(chapterToolbar)
        unpackExtras()

        chapterToolbar.title = searchResult?.name
        loadChapters()
    }

    private fun loadChapters(): Unit{
        val manga = mangaSource
        val search = searchResult

        if(manga != null && search != null){
            updateChapterResults(emptyList(), true)
            manga.getChapters(search.url, {chapters ->
                updateChapterResults(chapters, false)
            })
        }else{
            throw Error("mangaSource and searchResult need to be defined for chapterActivity to load chapters");
        }
    }



    private fun updateChapterResults(chapters: List<ChapterResult>, isLoading: Boolean):Unit{
        this@ChapterActivity.runOnUiThread{
            //todo loading spinnger magic here
            chapterRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
            chapterRecyclerView.adapter = ChapterResultListItemAdapter(chapters, ::onClickChapterResult)
        }
    }

    private fun onClickChapterResult(chapterResult: ChapterResult): Unit {
        print(chapterResult)
        /*
        val intent = Intent(this, ChapterActivity::class.java)
        intent.putExtra("mangaSource", mangaSource)
        intent.putExtra("searchResult", searchResult)
        startActivity(intent)
        */
    }



    private fun unpackExtras(): Unit{
        val extras = intent.extras?: throw Error("extras is missing")
        mangaSource =
            (extras.getSerializable("mangaSource") ?: throw Error("sourceName is missing")) as? MangaSource ?: throw Error("Serializable is no MangaSource")
        searchResult = (extras.getSerializable("searchResult")?: throw Error("searchResult is missing")) as? SearchResult ?: throw Error("Serializable is no SearchResult")
    }

}
