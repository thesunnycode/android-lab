package com.example.lab10contactsproviderdemo

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import android.util.Patterns

data class DeviceContact(
    val id: Long,
    val lookupKey: String,
    val name: String,
    val phones: List<String>,
    val emails: List<String>,
    val note: String?
) {
    val lookupUri: Uri get() = Contacts.getLookupUri(id, lookupKey)
}

/** Reads and writes the shared address book through the Contacts Provider. Call off the main thread. */
class ContactsRepository(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    /** Contacts sorted by name; a non-blank [filter] uses the provider's own name search. */
    fun all(filter: String = ""): List<DeviceContact> {
        val uri = if (filter.isBlank()) Contacts.CONTENT_URI
        else Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(filter.trim()))
        val contacts = mutableListOf<Triple<Long, String, String>>()
        resolver.query(uri, arrayOf(Contacts._ID, Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME_PRIMARY),
            null, null, "${Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC")?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(Contacts._ID)
            val keyColumn = cursor.getColumnIndexOrThrow(Contacts.LOOKUP_KEY)
            val nameColumn = cursor.getColumnIndexOrThrow(Contacts.DISPLAY_NAME_PRIMARY)
            while (cursor.moveToNext()) {
                contacts.add(Triple(cursor.getLong(idColumn), cursor.getString(keyColumn),
                    cursor.getString(nameColumn) ?: ""))
            }
        }
        if (contacts.isEmpty()) return emptyList()

        // One query on the Data table fetches every phone, email and note row, grouped by contact.
        val details = mutableMapOf<Long, MutableMap<String, MutableList<String>>>()
        resolver.query(Data.CONTENT_URI, arrayOf(Data.CONTACT_ID, Data.MIMETYPE, Data.DATA1),
            "${Data.MIMETYPE} IN (?, ?, ?)",
            arrayOf(Phone.CONTENT_ITEM_TYPE, Email.CONTENT_ITEM_TYPE, Note.CONTENT_ITEM_TYPE), null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(Data.CONTACT_ID)
            val typeColumn = cursor.getColumnIndexOrThrow(Data.MIMETYPE)
            val valueColumn = cursor.getColumnIndexOrThrow(Data.DATA1)
            while (cursor.moveToNext()) {
                val value = cursor.getString(valueColumn)?.takeIf { it.isNotBlank() } ?: continue
                details.getOrPut(cursor.getLong(idColumn)) { mutableMapOf() }
                    .getOrPut(cursor.getString(typeColumn)) { mutableListOf() }.add(value)
            }
        }
        return contacts.map { (id, key, name) ->
            val rows = details[id].orEmpty()
            DeviceContact(id, key, name, rows[Phone.CONTENT_ITEM_TYPE].orEmpty().distinct(),
                rows[Email.CONTENT_ITEM_TYPE].orEmpty().distinct(), rows[Note.CONTENT_ITEM_TYPE]?.firstOrNull())
        }
    }

    /** Inserts one raw contact and its data rows in a single batch. Returns the aggregate contact ID. */
    fun add(name: String, phone: String, email: String = "", note: String = ""): Long {
        require(name.isNotBlank()) { "Name is required" }
        require(isValidPhone(phone)) { "A valid phone number is required" }
        require(email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) { "Invalid email" }

        // A null account stores the contact locally on the device.
        val operations = arrayListOf(
            ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, null)
                .withValue(RawContacts.ACCOUNT_NAME, null).build(),
            dataRow(StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, name.trim()).build(),
            dataRow(Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, phone.trim())
                .withValue(Phone.TYPE, Phone.TYPE_MOBILE).build()
        )
        if (email.isNotBlank()) operations += dataRow(Email.CONTENT_ITEM_TYPE)
            .withValue(Email.ADDRESS, email.trim())
            .withValue(Email.TYPE, Email.TYPE_HOME).build()
        if (note.isNotBlank()) operations += dataRow(Note.CONTENT_ITEM_TYPE)
            .withValue(Note.NOTE, note.trim()).build()

        val rawContactId = ContentUris.parseId(
            checkNotNull(resolver.applyBatch(ContactsContract.AUTHORITY, operations)[0].uri))
        resolver.query(RawContacts.CONTENT_URI, arrayOf(RawContacts.CONTACT_ID),
            "${RawContacts._ID} = ?", arrayOf(rawContactId.toString()), null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        error("Contact $rawContactId was not aggregated")
    }

    /** Deletes the contact by lookup URI, so it still resolves if the provider re-aggregated it. */
    fun delete(contact: DeviceContact): Int = resolver.delete(contact.lookupUri, null, null)

    // Each data row points back to the raw contact created by the first operation of the batch.
    private fun dataRow(mimeType: String) = ContentProviderOperation.newInsert(Data.CONTENT_URI)
        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
        .withValue(Data.MIMETYPE, mimeType)

    companion object {
        fun isValidPhone(phone: String): Boolean {
            val trimmed = phone.trim()
            return trimmed.matches(Regex("[+0-9 ()-]{3,20}")) && trimmed.count { it.isDigit() } >= 3
        }
    }
}
