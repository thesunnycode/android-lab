package com.example.lab10contactsproviderdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var contacts: ContactsRepository
    private lateinit var providerStatus: TextView
    private lateinit var permissionCard: View
    private lateinit var permissionBody: TextView
    private lateinit var grantAccess: MaterialButton
    private lateinit var addContact: MaterialButton
    private lateinit var searchInput: TextInputEditText
    private lateinit var summary: TextView
    private lateinit var status: TextView
    private lateinit var contactList: LinearLayout

    // Provider calls run in order on one background thread; results are posted back to the UI.
    private val provider = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val reload = Runnable { loadContacts() }
    private var observing = false
    private var writing = false
    private var lastWriteAt = 0L

    // Fires when any app (including this one) changes the shared address book.
    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            // Our own insert/delete also notifies, sometimes after it completes; only label outside changes.
            if (!writing && SystemClock.uptimeMillis() - lastWriteAt > 2000) status.setText(R.string.refreshed)
            // The provider sends a burst of notifications per change; reload once it settles.
            handler.removeCallbacks(reload)
            handler.postDelayed(reload, 300)
        }
    }

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { onPermissionResult() }

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
        contacts = ContactsRepository(this)
        providerStatus = findViewById(R.id.providerStatus)
        permissionCard = findViewById(R.id.permissionCard)
        permissionBody = findViewById(R.id.permissionBody)
        grantAccess = findViewById(R.id.grantAccess)
        addContact = findViewById(R.id.addContact)
        searchInput = findViewById(R.id.searchInput)
        summary = findViewById(R.id.summary)
        status = findViewById(R.id.status)
        contactList = findViewById(R.id.contactList)

        grantAccess.setOnClickListener { requestAccess() }
        addContact.setOnClickListener { showNewContactSheet() }
        searchInput.doAfterTextChanged {
            handler.removeCallbacks(reload)
            handler.postDelayed(reload, 250)
        }
        if (hasAccess()) startProvider() else {
            showPermissionCard(denied = false)
            if (savedInstanceState == null) requestAccess()
        }
    }

    override fun onResume() {
        super.onResume()
        // Permission may have been granted or revoked in Settings while the app was in the background.
        if (hasAccess() && !observing) startProvider()
        else if (!hasAccess() && observing) {
            stopObserving()
            showPermissionCard(denied = true)
        }
    }

    private fun hasAccess() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAccess() {
        if (grantAccess.tag == SETTINGS) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        } else permissionRequest.launch(PERMISSIONS)
    }

    private fun onPermissionResult() {
        if (hasAccess()) startProvider() else showPermissionCard(denied = true)
    }

    private fun showPermissionCard(denied: Boolean) {
        permissionCard.visibility = View.VISIBLE
        addContact.isEnabled = false
        searchInput.isEnabled = false
        contactList.removeAllViews()
        summary.text = null
        status.text = null
        findViewById<View>(R.id.emptyContacts).visibility = View.GONE
        providerStatus.setText(R.string.provider_waiting)
        // When Android stops showing the dialog ("Don't allow" twice), only Settings can grant access.
        val blocked = denied && PERMISSIONS.none { shouldShowRequestPermissionRationale(it) }
        permissionBody.setText(if (denied) R.string.permission_denied else R.string.permission_body)
        grantAccess.setText(if (blocked) R.string.open_settings else R.string.grant_access)
        grantAccess.tag = if (blocked) SETTINGS else null
    }

    private fun startProvider() {
        permissionCard.visibility = View.GONE
        addContact.isEnabled = true
        searchInput.isEnabled = true
        if (!observing) {
            contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
            observing = true
        }
        status.setText(R.string.loaded)
        loadContacts()
    }

    private fun stopObserving() {
        if (observing) contentResolver.unregisterContentObserver(observer)
        observing = false
    }

    private fun loadContacts() {
        if (!hasAccess()) return
        val filter = searchInput.text.toString().trim()
        runProvider({ Pair(contacts.all(filter), if (filter.isEmpty()) null else contacts.all().size) }, { (found, total) ->
            // Ignore a result for a search the user has already changed.
            if (filter == searchInput.text.toString().trim()) renderContacts(found, filter, total ?: found.size)
        })
    }

    private fun renderContacts(found: List<DeviceContact>, filter: String, total: Int) {
        providerStatus.text = resources.getQuantityString(R.plurals.provider_count, total, total)
        summary.text = if (filter.isEmpty()) resources.getQuantityString(R.plurals.contact_summary, found.size, found.size)
        else resources.getQuantityString(R.plurals.search_summary, found.size, found.size, filter)
        val empty = findViewById<TextView>(R.id.emptyContacts)
        empty.visibility = if (found.isEmpty()) View.VISIBLE else View.GONE
        empty.text = if (filter.isEmpty()) getString(R.string.empty_contacts) else getString(R.string.empty_search, filter)
        contactList.removeAllViews()
        found.forEach { contact ->
            val row = layoutInflater.inflate(R.layout.row_contact, contactList, false)
            bindAvatar(row.findViewById(R.id.avatar), contact)
            row.findViewById<TextView>(R.id.contactName).text = contact.name
            val phone = contact.phones.firstOrNull()
            val email = contact.emails.firstOrNull()
            row.findViewById<TextView>(R.id.contactDetail).text =
                if (phone != null && email != null) getString(R.string.detail_pair, phone, email)
                else phone ?: email ?: getString(R.string.no_phone)
            row.findViewById<TextView>(R.id.contactNote).apply {
                text = contact.note
                visibility = if (contact.note == null) View.GONE else View.VISIBLE
            }
            row.contentDescription = listOfNotNull(contact.name, contact.phones.firstOrNull(), contact.note).joinToString(", ")
            row.setOnClickListener { showContactSheet(contact) }
            contactList.addView(row)
        }
    }

    @SuppressLint("InflateParams") // BottomSheetDialog supplies the parent in setContentView().
    private fun showNewContactSheet() {
        val sheet = BottomSheetDialog(this)
        val form = layoutInflater.inflate(R.layout.sheet_new_contact, null)
        sheet.setContentView(form)
        sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        sheet.behavior.skipCollapsed = true
        val name = form.findViewById<TextInputEditText>(R.id.nameInput)
        val phone = form.findViewById<TextInputEditText>(R.id.phoneInput)
        val email = form.findViewById<TextInputEditText>(R.id.emailInput)
        val note = form.findViewById<TextInputEditText>(R.id.noteInput)
        val save = form.findViewById<MaterialButton>(R.id.saveContact)
        val cancel = form.findViewById<MaterialButton>(R.id.cancelContact)
        cancel.setOnClickListener { sheet.dismiss() }
        save.setOnClickListener {
            val values = listOf(name, phone, email, note).map { it.text.toString().trim() }
            val nameError = if (values[0].isEmpty()) getString(R.string.name_required) else null
            val phoneError = if (!ContactsRepository.isValidPhone(values[1])) getString(R.string.phone_invalid) else null
            val emailError = if (values[2].isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(values[2]).matches())
                getString(R.string.email_invalid) else null
            form.findViewById<TextInputLayout>(R.id.nameLayout).error = nameError
            form.findViewById<TextInputLayout>(R.id.phoneLayout).error = phoneError
            form.findViewById<TextInputLayout>(R.id.emailLayout).error = emailError
            if (nameError != null || phoneError != null || emailError != null) return@setOnClickListener
            // Keep the form open until the insert succeeds, so a failed write can be retried.
            save.isEnabled = false
            cancel.isEnabled = false
            sheet.setCancelable(false)
            writing = true
            runProvider({ contacts.add(values[0], values[1], values[2], values[3]) }, {
                finishWrite()
                sheet.dismiss()
                searchInput.text = null
                status.text = getString(R.string.added, values[0])
                loadContacts()
            }, {
                finishWrite()
                save.isEnabled = true
                cancel.isEnabled = true
                sheet.setCancelable(true)
            })
        }
        sheet.show()
    }

    @SuppressLint("InflateParams") // BottomSheetDialog supplies the parent in setContentView().
    private fun showContactSheet(contact: DeviceContact) {
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.sheet_contact, null)
        sheet.setContentView(view)
        sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        sheet.behavior.skipCollapsed = true
        bindAvatar(view.findViewById(R.id.avatar), contact)
        view.findViewById<TextView>(R.id.contactName).text = contact.name
        view.findViewById<TextView>(R.id.contactId).text = getString(R.string.contact_id, contact.id)
        view.findViewById<TextView>(R.id.contactDetail).text = details(contact)
        view.findViewById<MaterialButton>(R.id.openContact).setOnClickListener {
            // The system Contacts app reads the same row this app inserted or queried.
            // Close the sheet: its details may be stale after edits made in the Contacts app.
            sheet.dismiss()
            try {
                startActivity(Intent(Intent.ACTION_VIEW, contact.lookupUri))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, R.string.no_contacts_app, Toast.LENGTH_LONG).show()
            }
        }
        view.findViewById<MaterialButton>(R.id.shareContact).setOnClickListener {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, contact.name)
                .putExtra(Intent.EXTRA_TEXT, "${contact.name}\n${details(contact)}")
            startActivity(Intent.createChooser(send, getString(R.string.share_title)))
        }
        view.findViewById<MaterialButton>(R.id.deleteContact).setOnClickListener {
            sheet.dismiss()
            confirmDelete(contact)
        }
        sheet.show()
    }

    private fun confirmDelete(contact: DeviceContact) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_title)
            .setMessage(getString(R.string.delete_message, contact.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                writing = true
                runProvider({ contacts.delete(contact) }, { deleted ->
                    finishWrite()
                    status.text = if (deleted > 0) getString(R.string.deleted, contact.name)
                    else getString(R.string.missing_contact)
                    loadContacts()
                }, { finishWrite() })
            }.show()
    }

    private fun finishWrite() {
        writing = false
        lastWriteAt = SystemClock.uptimeMillis()
    }

    private fun details(contact: DeviceContact) =
        (contact.phones + contact.emails + listOfNotNull(contact.note)).joinToString("\n")
            .ifEmpty { getString(R.string.no_phone) }

    private fun bindAvatar(avatar: TextView, contact: DeviceContact) {
        avatar.text = contact.name.split(' ').filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }.ifEmpty { "#" }
        avatar.backgroundTintList = ColorStateList.valueOf(AVATAR_COLORS[Math.floorMod(contact.name.hashCode(), AVATAR_COLORS.size)])
    }

    private fun <T> runProvider(action: () -> T, success: (T) -> Unit, failure: () -> Unit = {}) {
        provider.execute {
            val result = runCatching(action)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                result.fold(success, { error ->
                    Log.e("ContactBridge", "Contacts Provider operation failed", error)
                    Toast.makeText(this, R.string.provider_error, Toast.LENGTH_LONG).show()
                    failure()
                })
            }
        }
    }

    override fun onDestroy() {
        stopObserving()
        handler.removeCallbacks(reload)
        provider.shutdown()
        super.onDestroy()
    }

    private companion object {
        val PERMISSIONS = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
        const val SETTINGS = "settings"
        val AVATAR_COLORS = intArrayOf(0xFFB9472A.toInt(), 0xFF2F6F62.toInt(), 0xFF4B5A8A.toInt(),
            0xFF8A5A2B.toInt(), 0xFF7A3E68.toInt())
    }
}
