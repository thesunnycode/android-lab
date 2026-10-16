package com.example.lab09datapersistencedemo

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class StudySession(val id: Long, val title: String, val minutes: Int)

class StudyDbHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "study_ledger.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL CHECK(length(trim(title)) > 0),
                minutes INTEGER NOT NULL CHECK(minutes BETWEEN 1 AND 1440)
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 has no migration. Add a non-destructive migration before increasing it.
        error("A migration from $oldVersion to $newVersion must be implemented")
    }

    private fun values(title: String, minutes: Int): ContentValues {
        require(title.trim().isNotEmpty()) { "Study topic is required" }
        require(minutes in 1..1440) { "Duration must be between 1 and 1440 minutes" }
        return ContentValues().apply {
            put("title", title.trim())
            put("minutes", minutes)
        }
    }

    fun add(title: String, minutes: Int): Long =
        writableDatabase.insertOrThrow("sessions", null, values(title, minutes))

    fun all(): List<StudySession> {
        val result = mutableListOf<StudySession>()
        readableDatabase.query("sessions", arrayOf("id", "title", "minutes"),
            null, null, null, null, "id DESC").use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow("id")
            val titleColumn = cursor.getColumnIndexOrThrow("title")
            val minutesColumn = cursor.getColumnIndexOrThrow("minutes")
            while (cursor.moveToNext()) {
                result.add(StudySession(cursor.getLong(idColumn), cursor.getString(titleColumn),
                    cursor.getInt(minutesColumn)))
            }
        }
        return result
    }

    fun update(id: Long, title: String, minutes: Int): Int =
        writableDatabase.update("sessions", values(title, minutes), "id = ?", arrayOf(id.toString()))

    fun delete(id: Long): Int =
        writableDatabase.delete("sessions", "id = ?", arrayOf(id.toString()))
}
