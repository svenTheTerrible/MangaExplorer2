package com.example.mangaexplorer2.activities

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Insets
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaexplorer2.R
import com.example.mangaexplorer2.adapters.DoubleFavoritenListItemAdapter
import com.example.mangaexplorer2.adapters.FavoritenListItemAdapter
import com.example.mangaexplorer2.databinding.ActivityMainBinding
import com.example.mangaexplorer2.mangaSources.*
import com.example.mangaexplorer2.mangaSources.util.*
import com.example.mangaexplorer2.models.FavoriteItem
import com.example.mangaexplorer2.models.NextChapterState
import com.example.mangaexplorer2.utility.FavoritenDB
import com.example.mangaexplorer2.utility.closeFavoriteDbInstance
import com.example.mangaexplorer2.utility.getFavoriteDbInstance
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding;


    private var favoriteItems: List<FavoriteItem> = emptyList()
    private lateinit var favoritenDB: FavoritenDB

    private var isCheckingForUpdates = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.includedAppBar.chapterToolbar)

        binding.includedAppBar.fab.setOnClickListener { view ->
            if (!isCheckingForUpdates) {
                checkForNewChapters(view)
            }
        }

        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.includedAppBar.chapterToolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener(this)
        favoritenDB = getFavoriteDbInstance(applicationContext)
        updateFavoriteList()
    }

    override fun onPause() {
        super.onPause()
        closeFavoriteDbInstance()
    }

    private fun checkFavoriteForUpdate(favorite: FavoriteItem): FavoriteItem {
        val mangaSource = SourceRegister.getSourceByString(favorite.mangaSource)
        var nextPageState = NextChapterState.UNAVAILABLE
        try {
            val nextPagePackage =
                mangaSource.getPageResultSync(favorite.currentPageUrl, favorite.chapterMenuUrl, favorite.mangaTitle)
            if (nextPagePackage.nextPageUrl != null) {
                nextPageState = NextChapterState.AVAILABLE
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

    private fun updateNewChapterFab(): Unit {
        if (isCheckingForUpdates) {
            binding.includedAppBar.fab.setImageDrawable(resources.getDrawable(R.drawable.baseline_public_24_white))
            binding.includedAppBar.fab.isEnabled = false
        } else {
            binding.includedAppBar.fab.setImageDrawable(resources.getDrawable(R.drawable.refresh))
            binding.includedAppBar.fab.isEnabled = true
        }
    }

    private fun checkForNewChapters(view: View): Unit {
        val handler = Handler(Looper.getMainLooper())
        isCheckingForUpdates = true
        updateNewChapterFab()
        Thread {
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
                val newChaptersCount =
                    updatedFavorites.filter { it.hasNewChapter == NextChapterState.AVAILABLE }.size
                Snackbar.make(
                    view,
                    "$newChaptersCount neue${if (newChaptersCount == 1) "s" else ""} Kapitel verfügbar",
                    Snackbar.LENGTH_LONG
                )
                    .setAction("Action", null).show()
                isCheckingForUpdates = false
                updateNewChapterFab()
                updateFavoriteList()
            }
        }.start()
    }

    private fun bundleBy2(favorites: List<FavoriteItem>): List<Pair<FavoriteItem, FavoriteItem?>> {
        val packedFavoriteItems = mutableListOf<Pair<FavoriteItem, FavoriteItem?>>()
        for (x in favoriteItems.indices step 2) {
            packedFavoriteItems.add(Pair(favorites[x], favorites.getOrNull(x+1)))
        }
        return packedFavoriteItems;
    }

    private fun getScreenWidth(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = this.windowManager.currentWindowMetrics
            val insets: Insets = windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            windowMetrics.bounds.width() - insets.left - insets.right
        } else {
            val displayMetrics = DisplayMetrics()
            this.windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.widthPixels
        }
    }

    private fun updateFavoriteList(): Unit {
        val density = resources.displayMetrics.density
        val width = getScreenWidth() / density

        favoriteItems = favoritenDB.getFavorites().sortedBy { item -> item.hasNewChapter }.reversed()
        binding.includedAppBar.includeContent.favoritenRecyclerView.layoutManager =
            LinearLayoutManager(
                this,
                RecyclerView.VERTICAL,
                false
            )

        if (width > 500) {
            val bundledFavoriteItems = bundleBy2(favoriteItems)
            binding.includedAppBar.includeContent.favoritenRecyclerView.adapter =
                DoubleFavoritenListItemAdapter(
                    bundledFavoriteItems,
                    ::onClickFavoriteItems,
                    ::openFavoriteMenu
                )
            return;
        }

        binding.includedAppBar.includeContent.favoritenRecyclerView.adapter =
            FavoritenListItemAdapter(
                favoriteItems,
                ::onClickFavoriteItems,
                ::openFavoriteMenu
            )
    }

    override fun onResume() {
        super.onResume()
        favoritenDB = getFavoriteDbInstance(applicationContext)
        updateFavoriteList()
    }

    private fun openToChapterMenu(favoriteItem: FavoriteItem): Unit {
        val intent = Intent(this, ChapterActivity::class.java)
        intent.putExtra("mangaSource", SourceRegister.getSourceByString(favoriteItem.mangaSource).clearAllCaches())
        intent.putExtra(
            "searchResult",
            SearchResult(
                favoriteItem.mangaTitle,
                favoriteItem.chapterMenuUrl,
                favoriteItem.coverImageUrl
            )
        )
        startActivity(intent)
    }

    private fun openFavoriteMenu(favoriteItem: FavoriteItem): Unit {
        val builder = AlertDialog.Builder(this)

        builder.setItems(listOf<String>("Kapitelmenü", "Defavorisieren").toTypedArray(), DialogInterface.OnClickListener(fun (
            _: DialogInterface, index: Int):Unit {
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
        val intent = Intent(this, PageReaderActivity::class.java)
        intent.putExtra("mangaSourceName", favoriteItem.mangaSource)
        intent.putExtra(
            "searchResult", SearchResult(
                name = favoriteItem.mangaTitle,
                url = favoriteItem.chapterMenuUrl,
                coverUrl = favoriteItem.coverImageUrl
            )
        )
        intent.putExtra(
            "starterUrl", favoriteItem.currentPageUrl
        )
        startActivity(intent)
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
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
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
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
            R.id.mangasource_mangakakalot -> {
                val intent = Intent(this, SearchActivity::class.java)
                intent.putExtra("mangaSource", MangaKakalot())
                startActivity(intent)
            }
            R.id.mangasource_mangadex -> {
                val intent = Intent(this, SearchActivity::class.java)
                intent.putExtra("mangaSource", Mangadex())
                startActivity(intent)
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
