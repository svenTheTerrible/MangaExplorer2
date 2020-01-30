package com.example.mangaexplorer2.Activities

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.widget.CircularProgressDrawable
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import com.example.mangaexplorer2.Activities.util.PageBufferList
import com.example.mangaexplorer2.Activities.util.PageBufferResult
import com.example.mangaexplorer2.Activities.util.PageLoadingState
import com.example.mangaexplorer2.Adapters.MultiImageViewAdapter
import com.example.mangaexplorer2.MangaSources.util.*
import com.example.mangaexplorer2.Models.FavoriteItem
import com.example.mangaexplorer2.Models.NextChapterState
import com.example.mangaexplorer2.R
import com.example.mangaexplorer2.Utility.FavoritenDB
import com.example.mangaexplorer2.Utility.closeFavoriteDbInstance
import com.example.mangaexplorer2.Utility.getFavoriteDbInstance
import kotlinx.android.synthetic.main.activity_page_reader.*


class PageReaderActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    private lateinit var mangaSource: MangaSource
    private lateinit var searchResult: SearchResult
    private lateinit var chapterResult: ChapterResult
    private lateinit var gDetector: GestureDetectorCompat

    private lateinit var pageBufferList: PageBufferList
    private var currentPageUrl: String? = null
    private var currentPageResult: PageBufferResult? = null
    private var lastPageRegister: Map<String, String> = mutableMapOf()

    private lateinit var favoriteDB: FavoritenDB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page_reader)
        pageBufferList = PageBufferList(applicationContext, 3)
        favoriteDB = getFavoriteDbInstance(applicationContext)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        unpackExtras()
        currentPageUrl = chapterResult.url
        supportActionBar?.hide()

        multiImageView.addOnItemTouchListener(object: RecyclerView.SimpleOnItemTouchListener(){
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gDetector.onTouchEvent(e)
                return super.onInterceptTouchEvent(rv, e)
            }
        })

        this.gDetector = GestureDetectorCompat(this, this)

        loadCurrentPageUrl()
    }

    override fun onResume() {
        super.onResume()
        favoriteDB = getFavoriteDbInstance(applicationContext)
    }

    override fun onPause() {
        super.onPause()
        closeFavoriteDbInstance()
        val currentPage = currentPageUrl
        if(currentPage != null){
            intent.putExtra(
                "chapterResult", ChapterResult(
                    name = "",
                    url = currentPage
                )
            )
        }
    }

    private fun unpackExtras() {
        val extras = intent.extras ?: throw Error("extras is missing")
        mangaSource =
            (extras.getSerializable("mangaSource") ?: throw Error("sourceName is missing")) as? MangaSource
                ?: throw Error("Serializable is no MangaSource")
        searchResult =
            (extras.getSerializable("searchResult") ?: throw Error("searchResult is missing")) as? SearchResult
                ?: throw Error("Serializable is no SearchResult")
        chapterResult =
            (extras.getSerializable("chapterResult") ?: throw Error("chapterResult is missing")) as? ChapterResult
                ?: throw Error("Serializable is no ChapterResult")
    }

    private fun loadCurrentPageUrl() {
        val pageUrl = currentPageUrl
        if (pageUrl != null) {
            progressBar.visibility = View.VISIBLE
            pageBufferList.createPageBuffer(pageUrl, searchResult.url, mangaSource)
            pageBufferList.getPageResult(pageUrl){
                updateImageView(it)
            }
        }
    }

    private fun updateImageView(pageBufferResult: PageBufferResult?) {
        progressBar.visibility = View.GONE
        if(pageBufferResult == null){
            return
        }
        val pageResult = pageBufferResult.pageResult
        when(pageBufferResult.loadingStatus){
            PageLoadingState.LOADED -> renderSuccessfullImage(pageBufferResult)
            else -> renderErrorOptions(pageBufferResult)
        }
        chapterNameTextView.text = pageResult?.chapterName?: ""
        pageCountTextView.text = (pageResult?.pageCount?.toString()?: "") + "/" + (pageResult?.pageAmount?.toString()?: "")
        if(pageBufferResult.imageBitmap.size > 1){
            renderBitmapList(pageBufferResult.imageBitmap)
        }else if(pageBufferResult.imageBitmap.size == 1){
            renderBitmap(pageBufferResult.imageBitmap[0])
        }
        pageBufferResult.imageBitmap
        this.currentPageResult = pageBufferResult
    }

    private fun renderErrorOptions(pageBufferResult: PageBufferResult){
        //todo give some options to fix this mess
    }

    private fun renderSuccessfullImage(pageBufferResult: PageBufferResult){
        val pageResult = pageBufferResult.pageResult!!
        chapterNameTextView.text = pageResult.chapterName
        pageCountTextView.text = pageResult.pageCount.toString() + "/" + pageResult.pageAmount.toString()
        if(pageBufferResult.imageBitmap.size > 1){
            renderBitmapList(pageBufferResult.imageBitmap)
        }else if(pageBufferResult.imageBitmap.size == 1){
            renderBitmap(pageBufferResult.imageBitmap[0])
        }
    }

    private fun renderBitmap(bitmap: Bitmap) {
        multiImageView.visibility = View.GONE
        singleImageView.visibility = View.VISIBLE
        singleImageView.setImageBitmap(bitmap)
    }

    private fun renderBitmapList(bitmaps: List<Bitmap>) {

        multiImageView.visibility = View.VISIBLE
        singleImageView.visibility = View.GONE
        multiImageView.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
        multiImageView.adapter = MultiImageViewAdapter(bitmaps)
    }

    private fun loadNextPage() {
        val lastPageUrl = currentPageUrl
        val nextPageUrl = currentPageResult?.pageResult?.nextPageUrl
        if (lastPageUrl != null && nextPageUrl != null) {
            lastPageRegister = lastPageRegister.plus(Pair(nextPageUrl, lastPageUrl))
        }
        currentPageUrl = nextPageUrl
        updateReadingProgress()
        loadCurrentPageUrl()
    }

    private fun updateReadingProgress(force: Boolean = false) {
        val currentUrl = currentPageUrl
        if (currentUrl != null && (force || favoriteDB.mangaIsFavorite(
                mangaSource.sourceName.toString(),
                searchResult.name
            ))
        ) {
            favoriteDB.saveReadingProgress(
                FavoriteItem(
                    mangaTitle = searchResult.name,
                    mangaSource = mangaSource.sourceName.toString(),
                    currentPageUrl = currentUrl,
                    chapterMenuUrl = searchResult.url,
                    hasNewChapter = NextChapterState.UNAVAILABLE,
                    coverImageUrl = searchResult.coverUrl
                )
            )
        }
    }

    private fun loadLastPage() {
        val lastPageUrl = lastPageRegister[currentPageUrl]
        if (lastPageUrl != null) {
            currentPageUrl = lastPageUrl
            loadCurrentPageUrl()
        }
    }

    // GESTURE STUFF DOWN HERE

    override fun onTouchEvent(event: MotionEvent): Boolean {
        this.gDetector.onTouchEvent(event)
        // Be sure to call the superclass implementation
        return super.onTouchEvent(event)
    }


    override fun onShowPress(e: MotionEvent?) {}

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return true
    }

    override fun onDown(e: MotionEvent?): Boolean {
        return true
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        if (e1 != null && e2 != null) {
            val xDiff = Math.abs(e1.x - e2.x)
            val yDiff = Math.abs(e1.y - e2.y)
            if (xDiff > yDiff) {
                if (xDiff > 100) {
                    if (e1.x > e2.x) {
                        //to right
                        loadLastPage()
                    } else {
                        //to left
                        loadNextPage()
                    }
                }
            }
        }
        return true
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        return true
    }

    override fun onLongPress(e: MotionEvent?) {
        val isFavorite = favoriteDB.mangaIsFavorite(mangaSource.sourceName.toString(), searchResult.name)

        val message = if (isFavorite) "Unfavorite manga?" else "Make manga favorite?"

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Favorization")
        builder.setMessage(message)

        builder.setPositiveButton(android.R.string.yes) { _, _ ->
            val view = window.decorView.rootView

            if (isFavorite) {
                favoriteDB.removeReadingProgress(mangaSource.sourceName.toString(), searchResult.name)
                Snackbar.make(view, "Manga removed from favorites", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            } else {
                updateReadingProgress(true)
                Snackbar.make(view, "Manga is now favorite", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
        }

        builder.setNegativeButton(android.R.string.no) { _, _ ->
        }
        builder.show()

    }

}
