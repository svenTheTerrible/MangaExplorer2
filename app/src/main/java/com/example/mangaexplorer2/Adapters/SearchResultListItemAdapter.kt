package com.example.mangaexplorer2.Adapters

import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.mangaexplorer2.GlideApp
import com.example.mangaexplorer2.MangaSources.util.SearchResult
import com.example.mangaexplorer2.R


/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 */
class SearchResultListItemAdapter(
    private val searchResults: List<SearchResult>,
    private val onClick: (SearchResult) -> Unit,
    private val referer: String
) : RecyclerView.Adapter<SearchResultListItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_result_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val searchResult: SearchResult = searchResults[position]
        val glideUrl = GlideUrl(
            searchResult.coverUrl,
            LazyHeaders.Builder().addHeader("Referer", referer).build()
        )
        GlideApp.with(holder.mView.context)
            .load(glideUrl).diskCacheStrategy(DiskCacheStrategy.NONE)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.searchResultImage)
        holder.mangaTitle.text = searchResult.name
        holder.resultContainer.setOnClickListener({
            onClick(searchResult)
        })
    }

    override fun getItemCount(): Int = searchResults.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val searchResultImage: ImageView = mView.findViewById(R.id.searchResultImage)
        val mangaTitle: TextView = mView.findViewById(R.id.mangaTitle)
        val resultContainer: FrameLayout = mView.findViewById(R.id.resultContainer)
    }
}
