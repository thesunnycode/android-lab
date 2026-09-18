package com.example.labtesttourplanner

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class TripPlannerActivity : AppCompatActivity(), DestinationFragment.OnAddToTripListener {

    companion object {
        private const val TAG = "TripPlannerActivity"
        private const val CHANNEL_ID = "tourplanner_channel"

        const val EXTRA_TRAVELER_NAME = "EXTRA_TRAVELER_NAME"
        const val EXTRA_DESTINATION = "EXTRA_DESTINATION"
        const val EXTRA_DAYS = "EXTRA_DAYS"
        const val EXTRA_CATEGORY = "EXTRA_CATEGORY"
        const val EXTRA_ACTIVITIES = "EXTRA_ACTIVITIES"
    }

    private lateinit var etTravelerName: EditText
    private lateinit var etDestination: EditText
    private lateinit var etDays: EditText

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_trip_planner)

        createNotificationChannel()
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

        etTravelerName = findViewById(R.id.etTravelerName)
        etDestination = findViewById(R.id.etDestination)
        etDays = findViewById(R.id.etDays)

        findViewById<RadioGroup>(R.id.rgCategory).setOnCheckedChangeListener { _, checkedId ->
            val category = when (checkedId) {
                R.id.rbHeritage -> "Heritage"
                R.id.rbRelaxation -> "Relaxation"
                else -> "Adventure"
            }
            loadDestinationFragment(category)
        }
    }

    private fun loadDestinationFragment(category: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, DestinationFragment.newInstance(category))
            .commit()
    }

    override fun onAddToTrip(category: String, activities: String) {
        val travelerName = etTravelerName.text.toString().trim()
        val destination = etDestination.text.toString().trim()
        val days = etDays.text.toString().trim()

        if (travelerName.isEmpty() || destination.isEmpty() || days.isEmpty()) {
            Toast.makeText(this, "Please fill traveler name, destination and days", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, ItineraryActivity::class.java).apply {
            putExtra(EXTRA_TRAVELER_NAME, travelerName)
            putExtra(EXTRA_DESTINATION, destination)
            putExtra(EXTRA_DAYS, days)
            putExtra(EXTRA_CATEGORY, category)
            putExtra(EXTRA_ACTIVITIES, activities)
        }
        startActivity(intent)
        sendAddedNotification(destination, category)
    }

    // A notification channel is mandatory on Android 8.0 (API 26) and above
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "TourPlanner Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "Notifications for destinations added to the itinerary"

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun sendAddedNotification(destination: String, category: String) {
        val permissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            return
        }

        val tapIntent = Intent(this, ItineraryActivity::class.java).apply {
            putExtra(EXTRA_TRAVELER_NAME, etTravelerName.text.toString().trim())
            putExtra(EXTRA_DESTINATION, destination)
            putExtra(EXTRA_DAYS, etDays.text.toString().trim())
            putExtra(EXTRA_CATEGORY, category)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Added to Itinerary")
            .setContentText("$destination ($category) was added to your trip")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        NotificationManagerCompat.from(this).notify(1, builder.build())
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
