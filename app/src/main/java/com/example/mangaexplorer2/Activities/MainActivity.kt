package com.example.mangaexplorer2.Activities

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import com.example.mangaexplorer2.Models.FavoriteItem
import com.example.mangaexplorer2.Adapters.FavoritenListItemAdapter
import com.example.mangaexplorer2.MangaSources.*
import com.example.mangaexplorer2.MangaSources.util.*
import com.example.mangaexplorer2.Models.NextChapterState
import com.example.mangaexplorer2.R
import com.example.mangaexplorer2.Utility.FavoritenDB
import com.example.mangaexplorer2.Utility.closeFavoriteDbInstance
import com.example.mangaexplorer2.Utility.getFavoriteDbInstance
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.lang.Exception

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var favoriteItems: List<FavoriteItem> = emptyList()
    private lateinit var favoritenDB: FavoritenDB

    private var isCheckingForUpdates = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(chapterToolbar)

        fab.setOnClickListener { view ->
            if(!isCheckingForUpdates){
                checkForNewChapters(view)
            }
        }

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, chapterToolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        favoritenDB = getFavoriteDbInstance(applicationContext)
        updateFavoriteList()
    }

    override fun onPause() {
        super.onPause()
        closeFavoriteDbInstance()
    }

    private fun checkFavoriteForUpdate(favorite: FavoriteItem): FavoriteItem {
        val mangaSource = SourceRegister().getSource(favorite.mangaSource)
        var nextPageState = NextChapterState.UNAVAILABLE
        try{
            val nextPagePackage = mangaSource.getPageResultSync(favorite.currentPageUrl, favorite.chapterMenuUrl)
            if(nextPagePackage.nextPageUrl != null){
                nextPageState =  NextChapterState.AVAILABLE
            }
        }catch (e:Exception){
            nextPageState = NextChapterState.ERROR
        }

        return FavoriteItem(
            favorite.mangaTitle,
            favorite.mangaSource,
            favorite.coverImageUrl,
            favorite.chapterMenuUrl,
            nextPageState,
            favorite.currentPageUrl
        )
    }

    private fun updateNewChapterFab(): Unit{
        if(isCheckingForUpdates){
            fab.setImageDrawable(resources.getDrawable(R.drawable.baseline_public_24_white))
            fab.isEnabled = false
        }else{
            fab.setImageDrawable(resources.getDrawable(R.drawable.refresh))
            fab.isEnabled = true
        }
    }

    private fun checkForNewChapters(view: View): Unit {
        val handler = Handler()
        isCheckingForUpdates = true
        updateNewChapterFab()
        Thread(Runnable {
            val updatedFavorites = favoriteItems.map {
                val updatedItem = checkFavoriteForUpdate(it)
                favoritenDB.setChapterAvailable(
                    updatedItem.hasNewChapter,
                    updatedItem.mangaSource,
                    updatedItem.mangaTitle
                )
                updatedItem
            }
            handler.post {
                favoriteItems = updatedFavorites
                val newChaptersCount = updatedFavorites.filter{ it.hasNewChapter == NextChapterState.AVAILABLE}.size
                Snackbar.make(view, "$newChaptersCount neue${if(newChaptersCount == 1) "s" else ""} Kapitel verfügbar", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
                isCheckingForUpdates = false
                updateNewChapterFab()
                updateFavoriteList()
            }
        }).start()
    }

    private fun updateFavoriteList(): Unit {
        favoriteItems = favoritenDB.getFavorites()
        favoritenRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
        favoritenRecyclerView.adapter =
            FavoritenListItemAdapter(favoriteItems, ::onClickFavoriteItems, ::openFavoriteMenu)
    }

    override fun onResume() {
        super.onResume()
        favoritenDB = getFavoriteDbInstance(applicationContext)
        updateFavoriteList()
    }

    private fun openToChapterMenu(favoriteItem: FavoriteItem): Unit {
        val intent = Intent(this, ChapterActivity::class.java)
        intent.putExtra("mangaSource", SourceRegister().getSource(favoriteItem.mangaSource))
        intent.putExtra(
            "searchResult",
            SearchResult(favoriteItem.mangaTitle, favoriteItem.chapterMenuUrl, favoriteItem.coverImageUrl)
        )
        startActivity(intent)
    }

    private fun openFavoriteMenu(favoriteItem: FavoriteItem): Unit {
        val builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater

        builder.setItems(listOf<String>("Kapitelmenü", "Defavorisieren").toTypedArray(), DialogInterface.OnClickListener(fun (dialogInterface: DialogInterface, index: Int):Unit {
            when (index) {
                0 -> openToChapterMenu(favoriteItem)
                1 -> {
                    favoritenDB.removeReadingProgress(favoriteItem.mangaSource, favoriteItem.mangaTitle)
                    updateFavoriteList()
                }
                else -> { // Note the block
                    throw Exception("Unknown option for favorite dialog")
                }
            }
        }))

        builder.create()
        builder.show()
    }

    private fun onClickFavoriteItems(favoriteItem: FavoriteItem): Unit {

        //todo schauen, ob chapterName wichtig ist

        val intent = Intent(this, PageReaderActivity::class.java)
        intent.putExtra("mangaSource", SourceRegister().getSource(favoriteItem.mangaSource))
        intent.putExtra(
            "searchResult", SearchResult(
                name = favoriteItem.mangaTitle,
                url = favoriteItem.chapterMenuUrl,
                coverUrl = favoriteItem.coverImageUrl
            )
        )
        intent.putExtra(
            "chapterResult", ChapterResult(
                name = "",
                url = favoriteItem.currentPageUrl
            )
        )
        startActivity(intent)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_favorites -> {
                // Handle the camera action
            }
            R.id.mangasource_mangatown -> {
                val intent = Intent(this, SearchActivity::class.java)
                intent.putExtra("mangaSource", MangaTown())
                startActivity(intent)
            }
            R.id.mangasource_tenmanga -> {
                val intent = Intent(this, SearchActivity::class.java)
                intent.putExtra("mangaSource", TenManga())
                startActivity(intent)
            }
            R.id.mangasource_bato -> {
                val intent = Intent(this, SearchActivity::class.java)
                intent.putExtra("mangaSource", Bato())
                startActivity(intent)
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
