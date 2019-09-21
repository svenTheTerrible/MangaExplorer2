package com.example.mangaexplorer2.Activities

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import com.example.mangaexplorer2.Models.FavoriteItem
import com.example.mangaexplorer2.Adapters.FavoritenListItemAdapter
import com.example.mangaexplorer2.MangaSources.*
import com.example.mangaexplorer2.MangaSources.util.*
import com.example.mangaexplorer2.R
import com.example.mangaexplorer2.Utility.FavoritenDB
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var favoriteItems: List<FavoriteItem> = emptyList()
    private lateinit var favoritenDB: FavoritenDB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(chapterToolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, chapterToolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        favoritenDB = FavoritenDB(applicationContext)
        updateFavoriteList()
    }

    fun updateFavoriteList(): Unit {
        favoriteItems = favoritenDB.getFavorites()
        favoritenRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
        favoritenRecyclerView.adapter = FavoritenListItemAdapter(favoriteItems, ::onClickFavoriteItems)
    }

    override fun onResume() {
        super.onResume()
        updateFavoriteList()
    }

    fun onClickFavoriteItems(favoriteItem: FavoriteItem): Unit {

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
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
