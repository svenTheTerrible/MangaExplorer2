package com.example.mangaexplorer2.Activities

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import com.example.mangaexplorer2.MangaSources.MangaSource
import com.example.mangaexplorer2.MangaSources.SourceRegister
import com.example.mangaexplorer2.R
import kotlinx.android.synthetic.main.activity_search_actitiy.*
import kotlinx.android.synthetic.main.app_bar_search_actitiy.*
import kotlinx.android.synthetic.main.content_search_actitiy.*

class SearchActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var searchText: String = ""
    private  var mangaSource?: MangaSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_actitiy)
        setSupportActionBar(toolbar)
        setMangaSource()
        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        searchEditText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                searchText = p0.toString();
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        searchEditText.setOnEditorActionListener{v, actionId, event->
            if(actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_UNSPECIFIED){
                runSearch(searchText)
                true
            }else{
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
        when (item.itemId) {
            R.id.nav_favorites -> {
                startActivity(Intent(this, MainActivity::class.java))
            }
            R.id.mangasource_mangatown -> {
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setMangaSource(): Unit{
        val extras = intent.extras?: throw Error("extras is missing")
        val sourceName = extras.getString("sourceName") ?: throw Error("sourceName is missing")
        mangaSource = SourceRegister().getSource(sourceName)
    }

    private fun runSearch(searchterm: String): Unit{
        print(searchterm)
    }
}
