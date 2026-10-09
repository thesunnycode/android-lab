package com.example.lab08listviewimageviewdemo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class DestinationAdapter(
    private val context: Context,
    private val destinations: List<Destination>
) : BaseAdapter() {

    private class ViewHolder(row: View) {
        val thumbnail: ImageView = row.findViewById(R.id.ivThumbnail)
        val name: TextView = row.findViewById(R.id.tvName)
        val tagline: TextView = row.findViewById(R.id.tvTagline)
        val category: TextView = row.findViewById(R.id.tvCategory)
    }

    override fun getCount(): Int = destinations.size

    override fun getItem(position: Int): Destination = destinations[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.row_destination, parent, false)
            .also { it.tag = ViewHolder(it) }

        val holder = row.tag as ViewHolder
        val destination = destinations[position]

        holder.thumbnail.setImageResource(destination.thumbnailRes)
        holder.name.text = destination.name
        holder.tagline.text = destination.tagline
        holder.category.text = destination.category

        return row
    }
}
