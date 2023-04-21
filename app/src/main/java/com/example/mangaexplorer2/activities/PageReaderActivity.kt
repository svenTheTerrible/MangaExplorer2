package com.example.mangaexplorer2.activities

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.core.view.GestureDetectorCompat
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.example.mangaexplorer2.adapters.MultiImageViewAdapter
import com.example.mangaexplorer2.mangaSources.util.*
import com.example.mangaexplorer2.models.FavoriteItem
import com.example.mangaexplorer2.models.NextChapterState
import com.example.mangaexplorer2.utility.FavoritenDB
import com.example.mangaexplorer2.utility.getFavoriteDbInstance
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import android.widget.Toast
import com.example.mangaexplorer2.activities.util.*
import com.example.mangaexplorer2.databinding.ActivityPageReaderBinding
import javax.xml.transform.Source
import kotlin.math.abs


class PageReaderActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    private lateinit var binding: ActivityPageReaderBinding;

    private lateinit var mangaSource: MangaSource
    private lateinit var searchResult: SearchResult
    private lateinit var gDetector: GestureDetectorCompat

    private lateinit var pageBufferList: PageBufferList
    private var currentPageUrl: String? = null
    private var currentPageResult: PageBufferResult? = null
    private var lastPageRegister: Map<String, String> = mutableMapOf()

    private lateinit var favoriteDB: FavoritenDB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println(savedInstanceState?.getInt("uff") ?: 0)
        binding = ActivityPageReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pageBufferList = PageBufferList(applicationContext, 3)
        favoriteDB = getFavoriteDbInstance(applicationContext)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        if(!unpackExtrasFromPreviousInstance(savedInstanceState)){
            unpackExtrasAndGetStarterUrl()
        }
        supportActionBar?.hide()
        binding.multiImageView.addOnItemTouchListener(object :
            RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gDetector.onTouchEvent(e)
                return super.onInterceptTouchEvent(rv, e)
            }
        })
        this.gDetector = GestureDetectorCompat(this, this)
        loadCurrentPageUrl()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putString("mangaSourceName", mangaSource.sourceName.toString())
        outState.putSerializable("searchResult", searchResult)
        outState.putSerializable("starterUrl", currentPageUrl)
        super.onSaveInstanceState(outState, outPersistentState)
    }

    private fun unpackExtrasFromPreviousInstance(savedInstanceState: Bundle?): Boolean{
        if(savedInstanceState == null){
            return false
        }
        val mangaSourceName = savedInstanceState.getString("mangaSourceName")
        val sResult = savedInstanceState.getSerializable("searchResult")
        val sUrl = savedInstanceState.getString("starterUrl")
        if(mangaSourceName == null || sResult == null || sUrl == null){
            return false
        }
        mangaSource = SourceRegister.getSourceByString(mangaSourceName)
        searchResult = sResult as SearchResult
        currentPageUrl = sUrl
        return true
    }

    private fun unpackExtrasAndGetStarterUrl() {
        val extras = intent.extras ?: throw Error("extras is missing")
        mangaSource = SourceRegister.getSourceByString(
            extras.getString("mangaSourceName") ?: throw Error("mangaSourceName not provided")
        )
        searchResult =
            (extras.getSerializable("searchResult")
                ?: throw Error("searchResult is missing")) as? SearchResult
                ?: throw Error("Serializable is no SearchResult")
        currentPageUrl = extras.getString("starterUrl") ?: throw Error("starter url is missing")
    }

    private fun loadCurrentPageUrl() {
        val pageUrl = currentPageUrl
        if (pageUrl != null) {
            binding.progressBar.visibility = View.VISIBLE
            pageBufferList.createPageBuffer(
                pageUrl,
                searchResult.url,
                mangaSource,
                searchResult.name
            )
            pageBufferList.getPageResult(pageUrl) {
                updateImageView(it)
            }
        }
    }

    private fun updateImageView(pageBuffer: PageBuffer?) {
        binding.progressBar.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE
        if (pageBuffer == null) {
            return
        }
        val result = pageBuffer.getResult()
        val pageResult = result.pageResult
        when (result.loadingStatus) {
            PageLoadingState.LOADED -> renderSuccessfullImage(result)
            else -> renderErrorOptions(pageBuffer)
        }
        binding.chapterNameTextView.text = pageResult?.chapterName ?: ""
        binding.pageCountTextView.text =
            (pageResult?.pageCount?.toString() ?: "") + "/" + (pageResult?.pageAmount?.toString()
                ?: "")
        this.currentPageResult = result
    }

    private fun initReloadButton(pageBuffer: PageBuffer): Unit {
        binding.reloadButton.setOnClickListener { _ ->
            binding.progressBar.visibility = View.VISIBLE
            binding.errorContainer.visibility = View.GONE
            pageBuffer.reloadFailedPage {
                updateImageView(pageBuffer)
            }
        }
    }

    private fun renderErrorOptions(pageBuffer: PageBuffer) {
        val result = pageBuffer.getResult()
        binding.errorContainer.visibility = View.VISIBLE
        binding.singleImageView.visibility = View.INVISIBLE
        binding.multiImageView.visibility = View.INVISIBLE
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (result.errorType === PageLoadingError.IMAGE_NOT_LOADING && result.pageResult?.imageUrl == null) {
            binding.errorText.text =
                "Image link could not be fetched"
            val clip = ClipData.newPlainText("", pageBuffer.pageUrl)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(applicationContext, "PageUrl copied", Toast.LENGTH_SHORT).show()
            initReloadButton(pageBuffer)
            return
        }

        if (result.errorType === PageLoadingError.IMAGE_NOT_LOADING) {
            binding.errorText.text =
                "Could not load image link -> imageUrl: '${result.pageResult?.imageUrl}'"
            val clip = ClipData.newPlainText("", result.pageResult?.imageUrl)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(applicationContext, "ImageUrl copied", Toast.LENGTH_SHORT).show()
            initReloadButton(pageBuffer)
            return
        }

        if (result.errorType === PageLoadingError.PAGE_NOT_LOADING) {
            binding.errorText.text = "Page did not load -> pageUrl: '${pageBuffer.pageUrl}'"
            val clip = ClipData.newPlainText("", pageBuffer.pageUrl)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(applicationContext, "PageUrl copied", Toast.LENGTH_SHORT).show()
            initReloadButton(pageBuffer)
            return
        }
    }

    private fun renderSuccessfullImage(pageBufferResult: PageBufferResult) {
        val pageResult = pageBufferResult.pageResult!!
        binding.chapterNameTextView.text = pageResult.chapterName
        binding.pageCountTextView.text =
            pageResult.pageCount.toString() + "/" + pageResult.pageAmount.toString()
        if (pageBufferResult.imageBitmap.size > 1) {
            renderBitmapList(pageBufferResult.imageBitmap)
        } else if (pageBufferResult.imageBitmap.size == 1) {
            renderBitmap(pageBufferResult.imageBitmap[0])
        }
    }

    private fun renderBitmap(bitmap: Bitmap) {
        binding.multiImageView.visibility = View.GONE
        binding.singleImageView.visibility = View.VISIBLE
        binding.singleImageView.setImageBitmap(bitmap)
    }

    private fun renderBitmapList(bitmaps: List<Bitmap>) {

        binding.multiImageView.visibility = View.VISIBLE
        binding.singleImageView.visibility = View.GONE
        binding.multiImageView.layoutManager =
            LinearLayoutManager(
                this,
                RecyclerView.VERTICAL,
                false
            )
        binding.multiImageView.adapter = MultiImageViewAdapter(bitmaps)
    }

    private fun loadNextPage() {
        val lastPageUrl = currentPageUrl
        val nextPageUrl = currentPageResult?.pageResult?.nextPageUrl
        if (lastPageUrl != null && nextPageUrl != null) {
            lastPageRegister = lastPageRegister.plus(Pair(nextPageUrl, lastPageUrl))
        }
        if (nextPageUrl != null) {
            currentPageUrl = nextPageUrl
            updateReadingProgress()
            loadCurrentPageUrl()
        }

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

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (e1 != null && e2 != null) {
            val xDiff = abs(e1.x - e2.x)
            val yDiff = abs(e1.y - e2.y)
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

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return true
    }

    override fun onLongPress(e: MotionEvent?) {
        val isFavorite =
            favoriteDB.mangaIsFavorite(mangaSource.sourceName.toString(), searchResult.name)

        val message = if (isFavorite) "Unfavorite manga?" else "Make manga favorite?"

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Favorization")
        builder.setMessage(message)

        builder.setPositiveButton(android.R.string.yes) { _, _ ->
            val view = window.decorView.rootView

            if (isFavorite) {
                favoriteDB.removeReadingProgress(
                    mangaSource.sourceName.toString(),
                    searchResult.name
                )
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
