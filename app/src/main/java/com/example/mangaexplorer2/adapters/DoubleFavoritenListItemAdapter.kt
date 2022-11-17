package com.example.mangaexplorer2.adapters

import android.content.Context
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
        favoriteItemToViewHolder(
            favoritePair.first,
            holder.viewHolderPair.first,
            holder.mView.context
        )
        favoriteItemToViewHolder(
            favoritePair.second,
            holder.viewHolderPair.second,
            holder.mView.context
        )
    }

    private fun favoriteItemToViewHolder(
        favorite: FavoriteItem?,
        holder: ViewHolderBundle,
        context: Context
    ): Unit {
        if (favorite == null) {
            holder.favoriteCardView.visibility = View.INVISIBLE
            return
        }else{
            holder.favoriteCardView.visibility = View.VISIBLE
        }

        val glideUrl = GlideUrl(
            favorite.coverImageUrl,
            LazyHeaders.Builder()
                .addHeader("Referer", SourceRegister().getSource(favorite.mangaSource).refererUrl)
                .build()
        )

        GlideApp.with(context)
            .load(glideUrl)
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

    private fun updateNewChapterItem(
        status: NextChapterState,
        holder: ViewHolderBundle,
        context: Context
    ): Unit {
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
