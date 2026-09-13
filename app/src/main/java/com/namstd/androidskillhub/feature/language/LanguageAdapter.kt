package com.namstd.androidskillhub.feature.language

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.namstd.androidskillhub.R
import com.namstd.androidskillhub.core.language.AppLanguage
import com.namstd.androidskillhub.core.ui.base.BaseRecyclerAdapter

class LanguageAdapter(
    languages: List<AppLanguage>,
    private var selected: AppLanguage,
    private val onSelect: (AppLanguage) -> Unit,
) : BaseRecyclerAdapter<AppLanguage, LanguageAdapter.TextHolder>(languages) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false) as TextView
        return TextHolder(view)
    }

    override fun bind(holder: TextHolder, item: AppLanguage, position: Int) {
        holder.bind(item.displayName, isSelected = item == selected) {
            val previous = items.indexOf(selected)
            selected = item
            notifyItemChanged(previous)
            notifyItemChanged(position)
            onSelect(item)
        }
    }

    class TextHolder(private val text: TextView) : RecyclerView.ViewHolder(text) {
        fun bind(label: String, isSelected: Boolean, onClick: () -> Unit) {
            text.text = label
            text.setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
            text.setOnClickListener { onClick() }
        }
    }
}
