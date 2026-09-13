package com.namstd.androidskillhub.core.ui.base

import androidx.recyclerview.widget.RecyclerView

abstract class BaseRecyclerAdapter<T, VH : RecyclerView.ViewHolder>(
    protected val items: List<T>,
) : RecyclerView.Adapter<VH>() {

    final override fun getItemCount() = items.size

    final override fun onBindViewHolder(holder: VH, position: Int) = bind(holder, items[position], position)

    protected abstract fun bind(holder: VH, item: T, position: Int)
}
