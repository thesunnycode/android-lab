package com.example.lab10contactsproviderdemo

import android.Manifest
import android.provider.ContactsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Contacts Provider tests. Every test contact uses the [PREFIX] name and is removed afterwards. */
@RunWith(AndroidJUnit4::class)
class ContactsProviderTest {
    @get:Rule val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val contacts = ContactsRepository(context)

    @Before @After fun removeTestContacts() {
        contacts.all(PREFIX).filter { it.name.startsWith(PREFIX) }.forEach { contacts.delete(it) }
    }

    @Test fun insertedContactIsReadBackWithAllDataRows() {
        val id = contacts.add("$PREFIX Sunny Kr Singh", "+91 98765 43210", "sunny@example.com", "USN 25MCAR0230")
        val saved = contacts.all().single { it.id == id }
        assertEquals("$PREFIX Sunny Kr Singh", saved.name)
        assertEquals(listOf("+91 98765 43210"), saved.phones)
        assertEquals(listOf("sunny@example.com"), saved.emails)
        assertEquals("USN 25MCAR0230", saved.note)
    }

    @Test fun contactIsVisibleToOtherAppsThroughTheProvider() {
        val id = contacts.add("$PREFIX Shared Contact", "080 1234 5678")
        // Query the provider directly, as any other app with READ_CONTACTS would, without this app's repository.
        context.contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY, ContactsContract.Contacts.HAS_PHONE_NUMBER),
            "${ContactsContract.Contacts._ID} = ?", arrayOf(id.toString()), null).use { cursor ->
            assertNotNull(cursor)
            assertTrue(cursor!!.moveToFirst())
            assertEquals("$PREFIX Shared Contact", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
        }
        // The provider's name filter finds it, and a non-matching filter does not.
        assertTrue(contacts.all("$PREFIX Shared").any { it.id == id })
        assertTrue(contacts.all("$PREFIX Nobody").none { it.id == id })
    }

    @Test fun deleteRemovesContactAndInvalidInputIsRejected() {
        val id = contacts.add("$PREFIX Temporary", "12345")
        val saved = contacts.all().single { it.id == id }
        assertEquals(1, contacts.delete(saved))
        assertTrue(contacts.all().none { it.id == id })
        assertEquals(0, contacts.delete(saved))
        assertThrows(IllegalArgumentException::class.java) { contacts.add(" ", "12345") }
        assertThrows(IllegalArgumentException::class.java) { contacts.add("$PREFIX Bad Phone", "ab") }
        assertThrows(IllegalArgumentException::class.java) { contacts.add("$PREFIX Bad Email", "12345", "not-an-email") }
        assertTrue(contacts.all(PREFIX).isEmpty())
    }

    private companion object {
        const val PREFIX = "ZZTest"
    }
}
