package com.example.lab09datapersistencedemo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android storage tests. Run on a dedicated emulator: setup clears this app's data. */
@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun resetStorage() {
        context.deleteDatabase("study_ledger.db")
        context.getSharedPreferences("student_profile", 0).edit().clear().commit()
    }

    @After fun cleanStorage() = resetStorage()

    @Test fun profileSurvivesNewStoreInstance() {
        assertNull(ProfilePreferences(context).load())
        assertTrue(ProfilePreferences(context).save(" Sunny Kr Singh ", "25mcar0230"))
        assertEquals(StudentProfile("Sunny Kr Singh", "25MCAR0230"), ProfilePreferences(context).load())
        assertThrows(IllegalArgumentException::class.java) {
            ProfilePreferences(context).save(" ", "25MCAR0230")
        }
        assertEquals("Sunny Kr Singh", ProfilePreferences(context).load()?.name)
    }

    @Test fun insertedRecordsSurviveDatabaseReopen() {
        var firstId = 0L
        var secondId = 0L
        StudyDbHelper(context).use { db ->
            firstId = db.add("SharedPreferences revision", 30)
            secondId = db.add("SQLite practice", 45)
            assertTrue(firstId > 0)
            assertTrue(secondId > firstId)
        }
        StudyDbHelper(context).use { db ->
            assertEquals(listOf(
                StudySession(secondId, "SQLite practice", 45),
                StudySession(firstId, "SharedPreferences revision", 30)
            ), db.all())
        }
    }

    @Test fun updatesAndDeletesPersistWithoutChangingOtherRecords() {
        var keptId = 0L
        StudyDbHelper(context).use { db ->
            keptId = db.add("SQLite practice", 45)
            val removedId = db.add("SharedPreferences revision", 30)
            assertEquals(1, db.update(keptId, "SQLite CRUD practice", 60))
            assertEquals(1, db.delete(removedId))
            assertEquals(0, db.delete(removedId))
            assertEquals(0, db.update(removedId, "Missing record", 10))
            assertThrows(IllegalArgumentException::class.java) { db.add(" ", 10) }
            assertThrows(IllegalArgumentException::class.java) { db.add("Invalid", 0) }
            assertThrows(IllegalArgumentException::class.java) { db.update(keptId, "Invalid", -1) }
        }
        StudyDbHelper(context).use { db ->
            assertEquals(listOf(StudySession(keptId, "SQLite CRUD practice", 60)), db.all())
        }
    }
}
