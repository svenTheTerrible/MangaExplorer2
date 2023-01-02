package com.example.mangaexplorer2.adapters
import androidx.core.content.ContextCompat
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.example.mangaexplorer2.GlideApp
import com.example.mangaexplorer2.mangaSources.util.SourceRegister
import com.example.mangaexplorer2.models.FavoriteItem
import com.example.mangaexplorer2.models.NextChapterState
import com.example.mangaexplorer2.R


/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 */
class FavoritenListItemAdapter(private val favoriteItems: List<FavoriteItem>, private val onClick:(FavoriteItem)-> Unit,private val onCLonglick:(FavoriteItem)-> Unit) : RecyclerView.Adapter<FavoritenListItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.favoriten_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val favoriteItem: FavoriteItem = favoriteItems[position]

        val glideUrl = GlideUrl(
            favoriteItem.coverImageUrl,
            LazyHeaders.Builder()
                .addHeader("Referer", SourceRegister.getSourceByString(favoriteItem.mangaSource).refererUrl)
                .build()
        )
        GlideApp.with(holder.mView.context)
            .load(glideUrl)
            .transition(withCrossFade())
            .into(holder.coverImageView)

        holder.sourceTextView.text = favoriteItem.mangaSource
        holder.titleTextView.text =favoriteItem.mangaTitle
        updateNewChapterItem(favoriteItem.hasNewChapter, holder)
        holder.favoriteCardView.setOnClickListener{
            onClick(favoriteItem)
        }

        holder.favoriteCardView.setOnLongClickListener(object: View.OnLongClickListener{
            override fun onLongClick(v: View?): Boolean {
                onCLonglick(favoriteItem)
                return true
            }
        })
    }

    private fun updateNewChapterItem(status: NextChapterState, holder: ViewHolder):Unit {
        holder.updateAvailableFrameLayout.visibility = View.INVISIBLE
        holder.updateAvailableFrameLayout.setBackgroundColor(ContextCompat.getColor(holder.mView.context, R.color.design_default_color_primary))
        holder.updateAvailableTextView.text = "new"

        if(status == NextChapterState.AVAILABLE){
            holder.updateAvailableFrameLayout.visibility = View.VISIBLE
            return
        }

        if(status == NextChapterState.ERROR){
            holder.updateAvailableFrameLayout.visibility = View.VISIBLE
            holder.updateAvailableTextView.text = "error"
            holder.updateAvailableFrameLayout.setBackgroundColor(ContextCompat.getColor(holder.mView.context, R.color.colorAccent))
            return
        }
    }

    override fun getItemCount(): Int = favoriteItems.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val coverImageView: ImageView = mView.findViewById(R.id.coverImageView)
        val titleTextView: TextView = mView.findViewById(R.id.titleTextView)
        val sourceTextView: TextView = mView.findViewById(R.id.sourceTextView)
        val updateAvailableFrameLayout: FrameLayout = mView.findViewById(R.id.updateAvailableFrameLayout)
        val updateAvailableTextView: TextView = mView.findViewById(R.id.updateAvailableTextView)
        val favoriteCardView: CardView = mView.findViewById(R.id.favoriteCardView)
    }
}
