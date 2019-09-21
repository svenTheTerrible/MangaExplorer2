package com.example.mangaexplorer2.Activities

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
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
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mangaexplorer2.Adapters.MultiImageViewAdapter
import com.example.mangaexplorer2.GlideApp
import com.example.mangaexplorer2.MangaSources.util.ChapterResult
import com.example.mangaexplorer2.MangaSources.util.MangaSource
import com.example.mangaexplorer2.MangaSources.util.PageResult
import com.example.mangaexplorer2.MangaSources.util.SearchResult
import com.example.mangaexplorer2.Models.FavoriteItem
import com.example.mangaexplorer2.R
import com.example.mangaexplorer2.Utility.FavoritenDB
import kotlinx.android.synthetic.main.activity_page_reader.*


class PageReaderActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    private lateinit var mangaSource: MangaSource
    private lateinit var searchResult: SearchResult
    private lateinit var chapterResult: ChapterResult
    private lateinit var gDetector: GestureDetectorCompat

    private var currentPageUrl: String? = null
    private var currentPageResult: PageResult? = null
    private var lastPageRegister: Map<String, String> = mutableMapOf()

    private lateinit var favoriteDB: FavoritenDB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page_reader)
        favoriteDB = FavoritenDB(applicationContext)
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

    private fun unpackExtras(): Unit {
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
            mangaSource.getPageResult(pageUrl) { pageResult ->
                updateImageView(pageResult)
            }
        }
    }

    private fun updateImageView(pageResult: PageResult): Unit {
        this.currentPageResult = pageResult
        this@PageReaderActivity.runOnUiThread {
            chapterNameTextView.text = pageResult.chapterName
            pageCountTextView.text = pageResult.pageCount.toString() + "/" + pageResult.pageAmount.toString()
            renderImage(pageResult.imageUrl)
        }
    }

    private fun renderImage(imageUrl: String?): Unit {
        val circularProgressDrawable = CircularProgressDrawable(this)
        circularProgressDrawable.strokeWidth = 5f
        circularProgressDrawable.centerRadius = 30f
        circularProgressDrawable.start()
        GlideApp.with(this)
            .asBitmap()
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .load(Uri.parse(imageUrl))
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(p0: Drawable?) {}

                override fun onResourceReady(p0: Bitmap, p1: Transition<in Bitmap>?) {
                    progressBar.visibility = View.GONE
                    if (p0.height / p0.width > 2.5) {
                        renderBitmapList(splitBitmaps(p0))
                    } else {
                        renderBitmap(p0)
                    }
                }
            })
    }

    private fun renderBitmap(bitmap: Bitmap): Unit {
        multiImageView.visibility = View.GONE
        singleImageView.visibility = View.VISIBLE
        singleImageView.setImageBitmap(bitmap);
    }

    private fun renderBitmapList(bitmaps: List<Bitmap>): Unit {

        multiImageView.visibility = View.VISIBLE
        singleImageView.visibility = View.GONE
        multiImageView.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
        multiImageView.adapter = MultiImageViewAdapter(bitmaps)
    }

    private fun splitBitmaps(origBitmap: Bitmap): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        val origHeight = origBitmap.height
        var processedHeight = 0
        while (processedHeight < origHeight) {
            val restHeight = origHeight - processedHeight
            val heightToUse = if (restHeight < 200) restHeight else 200
            bitmaps.add(Bitmap.createBitmap(origBitmap, 0, processedHeight, origBitmap.width, heightToUse))
            processedHeight += 200
        }
        return bitmaps
    }

    private fun loadNextPage() {
        val lastPageUrl = currentPageUrl
        val nextPageUrl = currentPageResult?.nextPageUrl
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
                    hasNewChapter = false,
                    coverImageUrl = searchResult.coverUrl
                )
            )
        }
    }

    private fun loadLastPage() {
        val lastPageUrl = lastPageRegister.get(currentPageUrl)
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
