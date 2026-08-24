package com.example.lab05notificationdemo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CHANNEL_ID = "lab05_notification_channel"
        const val EXTRA_TITLE = "NOTIF_TITLE"
        const val EXTRA_MESSAGE = "NOTIF_MESSAGE"
    }

    // Each notification gets its own id so multiple notifications can stack in the drawer
    private var nextNotificationId = 1

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etMessage = findViewById<EditText>(R.id.etMessage)

        findViewById<Button>(R.id.btnNotify).setOnClickListener {
            sendNotification(etTitle.text.toString(), etMessage.text.toString(), expandable = false)
        }

        findViewById<Button>(R.id.btnNotifyExpandable).setOnClickListener {
            sendNotification(etTitle.text.toString(), etMessage.text.toString(), expandable = true)
        }
    }

    // A notification channel is mandatory on Android 8.0 (API 26) and above
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lab 05 Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "Notifications sent from the Lab 05 demo app"

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun sendNotification(title: String, message: String, expandable: Boolean) {
        if (title.isBlank() || message.isBlank()) {
            Toast.makeText(this, "Enter both title and message", Toast.LENGTH_SHORT).show()
            return
        }

        // From Android 13 onwards a notification can only be posted once the user grants permission
        val permissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            Toast.makeText(this, "Please allow notifications to continue", Toast.LENGTH_SHORT).show()
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        val notificationId = nextNotificationId++

        // Tapping the notification opens DetailActivity carrying the same title and message
        val tapIntent = Intent(this, DetailActivity::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (expandable) {
            // BigTextStyle lets a long message expand to its full length in the drawer
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
        }

        NotificationManagerCompat.from(this).notify(notificationId, builder.build())
        Toast.makeText(this, "Notification sent", Toast.LENGTH_SHORT).show()
    }
}
