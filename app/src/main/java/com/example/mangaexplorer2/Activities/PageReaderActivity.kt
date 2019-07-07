package com.example.mangaexplorer2.Activities

import android.content.pm.ActivityInfo
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.app.AlertDialog
import android.view.GestureDetector
import android.view.MotionEvent
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.mangaexplorer2.GlideApp
import com.example.mangaexplorer2.MangaSources.ChapterResult
import com.example.mangaexplorer2.MangaSources.MangaSource
import com.example.mangaexplorer2.MangaSources.PageResult
import com.example.mangaexplorer2.MangaSources.SearchResult
import com.example.mangaexplorer2.Models.FavoriteItem
import com.example.mangaexplorer2.R
import com.example.mangaexplorer2.Utility.FavoritenDB
import kotlinx.android.synthetic.main.activity_page_reader.*


class PageReaderActivity : AppCompatActivity(), GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    private var mangaSource: MangaSource? = null
    private var searchResult: SearchResult? = null
    private var chapterResult: ChapterResult? = null
    private var gDetector: GestureDetectorCompat? = null


    private var currentPageUrl: String? = null
    private var currentPageResult: PageResult? = null
    private var lastPageRegister: Map<String, String> = mutableMapOf()

    private var favoriteDB: FavoritenDB? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page_reader)
        favoriteDB = FavoritenDB(applicationContext)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        unpackExtras()
        currentPageUrl = chapterResult?.url
        supportActionBar?.hide()
        this.gDetector = GestureDetectorCompat(this, this)
        gDetector?.setOnDoubleTapListener(this)

        loadCurrentPageUrl()
    }

    private fun unpackExtras(): Unit{
        val extras = intent.extras?: throw Error("extras is missing")
        mangaSource =
            (extras.getSerializable("mangaSource") ?: throw Error("sourceName is missing")) as? MangaSource ?: throw Error("Serializable is no MangaSource")
        searchResult = (extras.getSerializable("searchResult")?: throw Error("searchResult is missing")) as? SearchResult ?: throw Error("Serializable is no SearchResult")
        chapterResult = (extras.getSerializable("chapterResult")?: throw Error("chapterResult is missing")) as? ChapterResult ?: throw Error("Serializable is no ChapterResult")
    }

    private fun loadCurrentPageUrl(){
        val pageUrl = currentPageUrl
        if(pageUrl != null){
            mangaSource?.getPageResult(pageUrl) { pageResult->
                updateImageView(pageResult)
            }
        }
    }

    private fun updateImageView(pageResult: PageResult):Unit{
        this.currentPageResult = pageResult
        this@PageReaderActivity.runOnUiThread{
            chapterNameTextView.text = pageResult.chapterName
            pageCountTextView.text = pageResult.pageCount?.toString() + "/" + pageResult.pageAmount?.toString()
         GlideApp.with(this)
             .load(Uri.parse(pageResult.imageUrl)).diskCacheStrategy(DiskCacheStrategy.NONE)
             .into(pageReaderImageView)
        }
    }

    private fun loadNextPage(){
        val lastPageUrl = currentPageUrl
        val nextPageUrl = currentPageResult?.nextPageUrl
        if(lastPageUrl != null && nextPageUrl != null){
            lastPageRegister = lastPageRegister.plus(Pair(nextPageUrl, lastPageUrl))
        }
        currentPageUrl = nextPageUrl
        updateReadingProgress()
        loadCurrentPageUrl()
    }

    private fun updateReadingProgress(force: Boolean = false){
        val mangaSourceName = mangaSource?.sourceName.toString()
        val mangaTitle = searchResult?.name
        val coverUrl = searchResult?.coverUrl
        val chapterMenuUrl = searchResult?.url
        val currentPageUrl = currentPageUrl
        val db = favoriteDB
        if(db != null && mangaTitle != null && coverUrl != null && chapterMenuUrl != null && currentPageUrl != null && (force || db.mangaIsFavorite(mangaTitle, mangaSourceName))){
            db.saveReadingProgress(FavoriteItem(
                mangaTitle = mangaTitle,
                mangaSource = mangaSourceName,
                currentPageUrl = currentPageUrl,
                chapterMenuUrl = chapterMenuUrl,
                hasNewChapter = false,
                coverImageUrl = coverUrl
            ))
        }
    }

    private fun loadLastPage() {
        val lastPageUrl  = lastPageRegister.get(currentPageUrl)
        if(lastPageUrl != null){
            currentPageUrl = lastPageUrl
            loadCurrentPageUrl()
        }
    }

    // GESTURE STUFF DOWN HERE

    override fun onTouchEvent(event: MotionEvent): Boolean {
        this.gDetector?.onTouchEvent(event)
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
        if(e1 != null && e2 != null){
            val xDiff = Math.abs(e1.x - e2.x)
            val yDiff = Math.abs(e1.y - e2.y)
            if(xDiff > yDiff){
                if(xDiff > 100){
                    if(e1.x > e2.x){
                        //to right
                        loadLastPage()
                    }else{
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

        val mangaTitle = searchResult!!.name
        val mangaSource = mangaSource!!.sourceName


        val isFavorite = favoriteDB!!.mangaIsFavorite(mangaTitle, mangaSource.toString())


        val message = if(isFavorite) "Unfavorite manga?" else "Make manga favorite?"

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Favorization")
        builder.setMessage(message)

        builder.setPositiveButton(android.R.string.yes) { dialog, which ->
            val view = window.decorView.rootView

            if(isFavorite){
                favoriteDB!!.removeReadingProgress(mangaSource.toString(), mangaTitle)
                Snackbar.make(view, "Manga removed from favorites", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }else{
                updateReadingProgress(true)
                Snackbar.make(view, "Manga is now favorite", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
        }

        builder.setNegativeButton(android.R.string.no) { dialog, which ->
        }
        builder.show()

    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
        return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        return true
    }

}
