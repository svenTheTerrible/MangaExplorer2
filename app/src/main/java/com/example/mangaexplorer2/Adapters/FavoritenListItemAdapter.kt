package com.example.mangaexplorer2.Adapters
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.mangaexplorer2.Models.FavoriteItem
import com.example.mangaexplorer2.R


/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 */
class FavoritenListItemAdapter(private val favoriteItems: List<FavoriteItem>) : RecyclerView.Adapter<FavoritenListItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.favoriten_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val favoriteItem: FavoriteItem = favoriteItems[position]
        holder.coverImageView.setImageDrawable(favoriteItem.coverImage)
        holder.sourceTextView.text = favoriteItem.mangaSource
        holder.titleTextView.text =favoriteItem.mangaTitle
        holder.updateAvailableFrameLayout.visibility = if(favoriteItem.hasNewChapter) View.VISIBLE else View.INVISIBLE
        holder.listDivider.visibility = if(favoriteItems.size -1 === position) View.GONE else View.VISIBLE
    }

    override fun getItemCount(): Int = favoriteItems.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val coverImageView: ImageView = mView.findViewById(R.id.coverImageView)
        val titleTextView: TextView = mView.findViewById(R.id.titleTextView)
        val sourceTextView: TextView = mView.findViewById(R.id.sourceTextView)
        val updateAvailableFrameLayout: FrameLayout = mView.findViewById(R.id.updateAvailableFrameLayout)
        val listDivider: View = mView.findViewById(R.id.listDivider)
    }
}
