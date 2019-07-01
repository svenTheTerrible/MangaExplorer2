package com.example.mangaexplorer2.Adapters
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.mangaexplorer2.MangaSources.SearchResult
import com.example.mangaexplorer2.R


/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 */
class SearchResultListItemAdapter(private val searchResults: List<SearchResult>, private val onClick:(SearchResult)-> Unit) : RecyclerView.Adapter<SearchResultListItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_result_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val searchResult: SearchResult = searchResults[position]
        // holder.searchResultImage.setImageDrawable()
        holder.mangaTitle.text = searchResult.name

        holder.resultContainer.setOnClickListener({
            onClick(searchResult)
        })
    }

    override fun getItemCount(): Int = searchResults.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        //val searchResultImage: ImageView = mView.findViewById(R.id.searchResultImage)
        val mangaTitle: TextView = mView.findViewById(R.id.mangaTitle)
        val resultContainer: FrameLayout = mView.findViewById(R.id.resultContainer)
    }
}
