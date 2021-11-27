package com.example.mangaexplorer2.Adapters
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.example.mangaexplorer2.MangaSources.util.ChapterResult
import com.example.mangaexplorer2.R


/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 */
class ChapterResultListItemAdapter(private val chapterResults: List<ChapterResult>, private val onClick:(ChapterResult)-> Unit) : RecyclerView.Adapter<ChapterResultListItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chapter_result_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chapterResult: ChapterResult = chapterResults[position]
        holder.name.text = chapterResult.name
        holder.rowContainer.setOnClickListener{
            onClick(chapterResult)
        }
    }

    override fun getItemCount(): Int = chapterResults.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val name: TextView = mView.findViewById(R.id.chapterRow)
        val rowContainer: FrameLayout = mView.findViewById(R.id.chapterFrame)
    }
}
