package com.example.mangaexplorer2.Activities

import android.content.Intent
import android.os.Bundle
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaexplorer2.Adapters.SearchResultListItemAdapter
import com.example.mangaexplorer2.MangaSources.MangaTown
import com.example.mangaexplorer2.MangaSources.TenManga
import com.example.mangaexplorer2.MangaSources.util.MangaSource
import com.example.mangaexplorer2.MangaSources.util.SearchResult
import com.example.mangaexplorer2.R
import kotlinx.android.synthetic.main.activity_search_actitiy.*
import kotlinx.android.synthetic.main.app_bar_search_actitiy.*
import kotlinx.android.synthetic.main.content_search_actitiy.*

class SearchActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var searchText: String = ""
    private lateinit var mangaSource: MangaSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_actitiy)
        setSupportActionBar(chapterToolbar)

        setMangaSource()
        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, chapterToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                searchText = p0.toString();
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        searchEditText.setOnEditorActionListener { _, actionId, keyEvent ->

            val enterKeydown =
                keyEvent != null && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == ACTION_DOWN

            val virtualKeyBoardEnter =
                actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_SEND

            if (enterKeydown || virtualKeyBoardEnter) {
                runSearch(searchText)
                true
            } else {
                false
            }
        }
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
        menuInflater.inflate(R.menu.search_actitiy, menu)
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
        //todo add way to switch between mangasources, best is to keep ative search
        when (item.itemId) {
            R.id.nav_favorites -> {
                startActivity(Intent(this, MainActivity::class.java))
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

    private fun setMangaSource(): Unit {
        val extras = intent.extras ?: throw Error("extras is missing")
        mangaSource =
            (extras.getSerializable("mangaSource") ?: throw Error("sourceName is missing")) as? MangaSource
                ?: throw Error("Serializable is no MangaSource")
    }

    private fun updateSearchResults(searchResults: List<SearchResult>, isLoading: Boolean): Unit {
        this@SearchActivity.runOnUiThread {
            searchProgressBar.visibility = if (isLoading) {
                View.VISIBLE
            } else {
                View.GONE
            }
            emptySearchText.visibility = if (searchResults.size > 0 || isLoading) {
                View.GONE
            } else {
                View.VISIBLE
            }
            searchResultRecyclerView.layoutManager =
                LinearLayoutManager(
                    this,
                    RecyclerView.VERTICAL,
                    false
                )
            searchResultRecyclerView.adapter = SearchResultListItemAdapter(searchResults, ::onClickSearchResult)
        }
    }

    private fun onClickSearchResult(searchResult: SearchResult): Unit {
        val intent = Intent(this, ChapterActivity::class.java)
        intent.putExtra("mangaSource", mangaSource)
        intent.putExtra("searchResult", searchResult)
        startActivity(intent)
    }

    private fun runSearch(searchterm: String) {
        updateSearchResults(emptyList(), true)
        mangaSource.getSearchResult(searchterm, { searchResult ->
            updateSearchResults(searchResult, false)
        })
    }
}
