package com.example.lab09datapersistencedemo

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.concurrent.Executors
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var preferences: ProfilePreferences
    private lateinit var database: StudyDbHelper
    private lateinit var nameInput: TextInputEditText
    private lateinit var usnInput: TextInputEditText
    private lateinit var profileStatus: TextView
    private lateinit var recordsStatus: TextView
    private lateinit var sessionList: LinearLayout
    private lateinit var saveProfile: MaterialButton
    private lateinit var addSession: MaterialButton

    // A single queue keeps database writes, subsequent reads and closing in order.
    private val storage = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.updatePadding(top = bars.top, bottom = maxOf(bars.bottom, keyboard.bottom))
            insets
        }
        preferences = ProfilePreferences(this)
        database = StudyDbHelper(this)
        nameInput = findViewById(R.id.nameInput)
        usnInput = findViewById(R.id.usnInput)
        profileStatus = findViewById(R.id.profileStatus)
        recordsStatus = findViewById(R.id.recordsStatus)
        sessionList = findViewById(R.id.sessionList)
        saveProfile = findViewById(R.id.saveProfile)
        addSession = findViewById(R.id.addSession)
        saveProfile.isEnabled = false
        addSession.isEnabled = false
        saveProfile.setOnClickListener { saveStudentProfile() }
        addSession.setOnClickListener { showSessionDialog() }

        runStorage({ Pair(preferences.load(), database.all()) }, { (profile, sessions) ->
            if (profile != null) {
                // Let Android restore unfinished form edits on Activity recreation.
                if (savedInstanceState == null) {
                    nameInput.setText(profile.name)
                    usnInput.setText(profile.usn)
                }
                profileStatus.text = getString(R.string.profile_restored, profile.name, profile.usn)
            }
            renderSessions(sessions)
            recordsStatus.setText(R.string.loaded)
            saveProfile.isEnabled = true
            addSession.isEnabled = true
        }, {
            profileStatus.setText(R.string.storage_error)
            recordsStatus.setText(R.string.storage_error)
            saveProfile.isEnabled = true
            addSession.isEnabled = true
        })
    }

    private fun saveStudentProfile() {
        val name = nameInput.text.toString().trim()
        val usn = usnInput.text.toString().trim()
        nameInput.error = if (name.isEmpty()) getString(R.string.name_required) else null
        usnInput.error = if (usn.isEmpty()) getString(R.string.usn_required) else null
        if (name.isEmpty() || usn.isEmpty()) return
        hideKeyboard()
        saveProfile.isEnabled = false
        runStorage({
            check(preferences.save(name, usn)) { "Profile write failed" }
            checkNotNull(preferences.load())
        }, { profile ->
            nameInput.setText(profile.name)
            usnInput.setText(profile.usn)
            profileStatus.text = getString(R.string.profile_saved, profile.name, profile.usn)
            saveProfile.isEnabled = true
        }, {
            profileStatus.setText(R.string.storage_error)
            saveProfile.isEnabled = true
        })
    }

    private fun showSessionDialog(session: StudySession? = null) {
        val form = layoutInflater.inflate(R.layout.dialog_session, null)
        val topic = form.findViewById<TextInputEditText>(R.id.topicInput)
        val minutes = form.findViewById<TextInputEditText>(R.id.minutesInput)
        session?.let {
            topic.setText(it.title)
            minutes.setText(String.format(Locale.ROOT, "%d", it.minutes))
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (session == null) R.string.add_session else R.string.edit_session)
            .setView(form)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
        dialog.setOnShowListener {
            // Override the default listener so invalid input doesn't dismiss the form.
            val save = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            save.setOnClickListener {
                val title = topic.text.toString().trim()
                val duration = minutes.text.toString().toIntOrNull()
                topic.error = if (title.isEmpty()) getString(R.string.topic_required) else null
                minutes.error = if (duration == null || duration !in 1..1440)
                    getString(R.string.minutes_invalid) else null
                if (title.isEmpty() || duration == null || duration !in 1..1440) return@setOnClickListener
                save.isEnabled = false
                // Keep the editor open until the write succeeds, so a failed write can be retried.
                dialog.setCancelable(false)
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).isEnabled = false
                runStorage({
                    if (session == null) database.add(title, duration)
                    else check(database.update(session.id, title, duration) == 1) { "Missing session" }
                    database.all()
                }, { sessions ->
                    dialog.dismiss()
                    renderSessions(sessions)
                    recordsStatus.setText(if (session == null) R.string.added else R.string.updated)
                }, {
                    save.isEnabled = true
                    dialog.setCancelable(true)
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).isEnabled = true
                })
            }
        }
        dialog.show()
    }

    private fun confirmDelete(session: StudySession) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_title)
            .setMessage(getString(R.string.delete_message, session.title))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                runStorage({
                    val deleted = database.delete(session.id)
                    Pair(deleted, database.all())
                }, { (deleted, sessions) ->
                    renderSessions(sessions)
                    recordsStatus.setText(if (deleted == 1) R.string.deleted else R.string.missing_record)
                })
            }.show()
    }

    private fun renderSessions(sessions: List<StudySession>) {
        findViewById<TextView>(R.id.summary).text = resources.getQuantityString(
            R.plurals.session_summary, sessions.size, sessions.size, sessions.sumOf { it.minutes.toLong() })
        findViewById<View>(R.id.emptySessions).visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
        sessionList.removeAllViews()
        sessions.forEach { session ->
            val row = layoutInflater.inflate(R.layout.row_session, sessionList, false)
            row.findViewById<TextView>(R.id.sessionTitle).text = session.title
            row.findViewById<TextView>(R.id.sessionDetail).text = getString(R.string.session_detail, session.id, session.minutes)
            row.findViewById<MaterialButton>(R.id.editSession).setOnClickListener { showSessionDialog(session) }
            row.findViewById<MaterialButton>(R.id.deleteSession).setOnClickListener { confirmDelete(session) }
            sessionList.addView(row)
        }
    }

    private fun hideKeyboard() {
        val view = currentFocus ?: return
        getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }

    private fun <T> runStorage(action: () -> T, success: (T) -> Unit, failure: () -> Unit = {}) {
        storage.execute {
            val result = runCatching(action)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                result.fold(success, { error ->
                    Log.e("StudyLedger", "Storage operation failed", error)
                    Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show()
                    failure()
                })
            }
        }
    }

    override fun onDestroy() {
        storage.execute { database.close() }
        storage.shutdown()
        super.onDestroy()
    }
}
