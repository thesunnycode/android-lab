package com.example.labtesttourplanner

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ItineraryActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ItineraryActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_itinerary)

        val travelerName = intent.getStringExtra(TripPlannerActivity.EXTRA_TRAVELER_NAME) ?: ""
        val destination = intent.getStringExtra(TripPlannerActivity.EXTRA_DESTINATION) ?: ""
        val days = intent.getStringExtra(TripPlannerActivity.EXTRA_DAYS) ?: ""
        val category = intent.getStringExtra(TripPlannerActivity.EXTRA_CATEGORY) ?: ""
        val activities = intent.getStringExtra(TripPlannerActivity.EXTRA_ACTIVITIES) ?: "Details available in the trip planner"

        val badgeRes = when (category) {
            "Heritage" -> R.drawable.badge_heritage
            "Relaxation" -> R.drawable.badge_relaxation
            else -> R.drawable.badge_adventure
        }

        findViewById<TextView>(R.id.tvCategoryBadge).apply {
            text = category
            setBackgroundResource(badgeRes)
        }
        findViewById<TextView>(R.id.tvDestination).text = destination
        findViewById<TextView>(R.id.tvTraveler).text = "Traveler: $travelerName"
        findViewById<TextView>(R.id.tvDays).text = "$days day(s) trip"
        findViewById<TextView>(R.id.tvActivitiesInfo).text = activities
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
}
