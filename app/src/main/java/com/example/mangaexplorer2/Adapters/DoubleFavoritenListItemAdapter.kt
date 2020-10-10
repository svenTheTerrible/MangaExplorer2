package com.example.mangaexplorer2.Adapters

import android.content.Context
import android.net.Uri
import android.opengl.Visibility
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.example.mangaexplorer2.GlideApp
import com.example.mangaexplorer2.Models.FavoriteItem
import com.example.mangaexplorer2.Models.NextChapterState
import com.example.mangaexplorer2.R


/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 */
class DoubleFavoritenListItemAdapter(
    private val favoriteItems: List<Pair<FavoriteItem, FavoriteItem?>>,
    private val onClick: (FavoriteItem) -> Unit,
    private val onLonglick: (FavoriteItem) -> Unit
) : RecyclerView.Adapter<DoubleFavoritenListItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.double_favoriten_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val favoritePair: Pair<FavoriteItem, FavoriteItem?> = favoriteItems[position]
        favoriteItemToViewHolder(favoritePair.first, holder.viewHolderPair.first, holder.mView.context)
        favoriteItemToViewHolder(favoritePair.second, holder.viewHolderPair.second, holder.mView.context)
    }

    private fun favoriteItemToViewHolder(favorite: FavoriteItem?, holder: ViewHolderBundle, context: Context): Unit {
        if(favorite == null){
            holder.favoriteCardView.visibility = View.INVISIBLE
            return
        }
        holder.favoriteCardView.visibility = View.VISIBLE
        GlideApp.with(context)
            .load(Uri.parse(favorite.coverImageUrl))
            .transition(withCrossFade())
            .into(holder.coverImageView)

        holder.sourceTextView.text = favorite.mangaSource
        holder.titleTextView.text = favorite.mangaTitle
        updateNewChapterItem(favorite.hasNewChapter, holder, context)
        holder.favoriteCardView.setOnClickListener {
            onClick(favorite)
        }

        holder.favoriteCardView.setOnLongClickListener {
            onLonglick(favorite)
            true
        }
    }

    private fun updateNewChapterItem(status: NextChapterState, holder: ViewHolderBundle, context: Context): Unit {
        holder.updateAvailableFrameLayout.visibility = View.INVISIBLE
        holder.updateAvailableFrameLayout.setBackgroundColor(
            ContextCompat.getColor(
                context,
                R.color.design_default_color_primary
            )
        )
        holder.updateAvailableTextView.text = "new"

        if (status == NextChapterState.AVAILABLE) {
            holder.updateAvailableFrameLayout.visibility = View.VISIBLE
            return
        }

        if (status == NextChapterState.ERROR) {
            holder.updateAvailableFrameLayout.visibility = View.VISIBLE
            holder.updateAvailableTextView.text = "error"
            holder.updateAvailableFrameLayout.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.colorAccent
                )
            )
            return
        }
    }

    override fun getItemCount(): Int = favoriteItems.size

    class ViewHolderBundle(
        val coverImageView: ImageView,
        val titleTextView: TextView,
        val sourceTextView: TextView,
        val updateAvailableFrameLayout: FrameLayout,
        val updateAvailableTextView: TextView,
        val favoriteCardView: CardView
    )

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val viewHolderPair: Pair<ViewHolderBundle, ViewHolderBundle> = Pair(
            ViewHolderBundle(
                mView.findViewById(R.id.leftCoverImageView),
                mView.findViewById(R.id.leftTitleTextView),
                mView.findViewById(R.id.leftSourceTextView),
                mView.findViewById(R.id.leftUpdateAvailableFrameLayout),
                mView.findViewById(R.id.leftUpdateAvailableTextView),
                mView.findViewById(R.id.leftFavoriteCardView)
            ), ViewHolderBundle(
                mView.findViewById(R.id.rightCoverImageView),
                mView.findViewById(R.id.rightTitleTextView),
                mView.findViewById(R.id.rightSourceTextView),
                mView.findViewById(R.id.rightUpdateAvailableFrameLayout),
                mView.findViewById(R.id.rightUpdateAvailableTextView),
                mView.findViewById(R.id.rightFavoriteCardView)
            )
        )
    }
}
