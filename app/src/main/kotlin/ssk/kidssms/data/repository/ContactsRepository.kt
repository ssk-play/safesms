package ssk.kidssms.data.repository

import android.content.Context
import android.provider.ContactsContract
import android.util.Log

class ContactsRepository(private val context: Context) {

    /**
     * Get contact name from phone number
     * Returns the contact name if found, otherwise returns null
     */
    fun getContactName(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?"
        val selectionArgs = arrayOf(phoneNumber)

        return try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        cursor.getString(nameIndex)
                    } else null
                } else null
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
