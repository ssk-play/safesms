package ssk.kidssms.data.repository

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log

class ContactsRepository(private val context: Context) {

    /**
     * Get contact name from phone number
     * Returns the contact name if found, otherwise returns null
     */
    fun getContactName(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null

        // Use PhoneLookup for better phone number matching
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME
        )

        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val name = cursor.getString(nameIndex)
                        Log.d("ContactsRepository", "Found contact name '$name' for $phoneNumber")
                        name
                    } else {
                        Log.d("ContactsRepository", "Display name column not found")
                        null
                    }
                } else {
                    Log.d("ContactsRepository", "No contact found for $phoneNumber")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ContactsRepository", "Failed to get contact name for $phoneNumber", e)
            null
        }
    }

    /**
     * Get display name for phone number
     * Returns contact name if found, otherwise returns the phone number itself
     */
    fun getDisplayName(phoneNumber: String): String {
        return getContactName(phoneNumber) ?: phoneNumber
    }
}
