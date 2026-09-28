package com.example.lab08listviewimageviewdemo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val destinations = DestinationRepository.all()

    // Present only in the sw600dp (tablet) layout; null on phones.
    private var detailPlaceholder: TextView? = null
    private var detailImage: ImageView? = null
    private var detailCategory: TextView? = null
    private var detailName: TextView? = null
    private var detailDescription: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val lvDestinations = findViewById<ListView>(R.id.lvDestinations)
        lvDestinations.adapter = DestinationAdapter(this, destinations)

        // These views only exist in res/layout-sw600dp/activity_main.xml.
        detailPlaceholder = findViewById(R.id.tvDetailPlaceholder)
        detailImage = findViewById(R.id.ivDetailImage)
        detailCategory = findViewById(R.id.tvDetailCategory)
        detailName = findViewById(R.id.tvDetailName)
        detailDescription = findViewById(R.id.tvDetailDescription)

        val isTwoPane = detailPlaceholder != null

        lvDestinations.setOnItemClickListener { _, _, position, _ ->
            val destination = destinations[position]
            if (isTwoPane) {
                showInDetailPanel(destination)
            } else {
                val intent = Intent(this, DestinationDetailActivity::class.java).apply {
                    putExtra(DestinationDetailActivity.EXTRA_NAME, destination.name)
                    putExtra(DestinationDetailActivity.EXTRA_CATEGORY, destination.category)
                    putExtra(DestinationDetailActivity.EXTRA_DESCRIPTION, destination.description)
                    putExtra(DestinationDetailActivity.EXTRA_HERO_RES, destination.heroRes)
                }
                startActivity(intent)
            }
        }
    }

    /** Two-pane (tablet) mode: fill the right-hand detail panel in place instead of navigating away. */
    private fun showInDetailPanel(destination: Destination) {
        detailPlaceholder?.visibility = View.GONE
        detailImage?.setImageResource(destination.heroRes)
        detailCategory?.apply {
            text = destination.category
            visibility = View.VISIBLE
        }
        detailName?.apply {
            text = destination.name
            visibility = View.VISIBLE
        }
        detailDescription?.apply {
            text = destination.description
            visibility = View.VISIBLE
        }
    }
}
