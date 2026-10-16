package com.example.lab09datapersistencedemo

import android.content.Context
import android.annotation.SuppressLint
import java.util.Locale

data class StudentProfile(val name: String, val usn: String)

class ProfilePreferences(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("student_profile", Context.MODE_PRIVATE)

    fun load(): StudentProfile? {
        val name = preferences.getString("name", null) ?: return null
        val usn = preferences.getString("usn", null) ?: return null
        return StudentProfile(name, usn)
    }

    /** Called on the storage executor: commit confirms the disk write before reporting success. */
    @SuppressLint("UseKtx") // Need commit()'s Boolean result; the KTX editor returns Unit.
    fun save(name: String, usn: String): Boolean {
        require(name.trim().isNotEmpty()) { "Name is required" }
        require(usn.trim().isNotEmpty()) { "USN is required" }
        return preferences.edit()
            .putString("name", name.trim())
            .putString("usn", usn.trim().uppercase(Locale.ROOT))
            .commit()
    }
}
