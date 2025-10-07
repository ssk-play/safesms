package ssk.kidssms.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import ssk.kidssms.data.model.SmsMessage
import ssk.kidssms.data.model.SmsThread

class SmsRepository(private val context: Context) {

    fun getAllThreads(): List<SmsThread> {
        val threadsMap = mutableMapOf<Long, SmsThread>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ
        )

        context.contentResolver.query(uri, projection, null, null, "${Telephony.Sms.DATE} DESC")?.use { cursor ->
            val threadIdIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val readIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)

            while (cursor.moveToNext()) {
                val threadId = cursor.getLong(threadIdIndex)

                // Only keep the most recent message per thread
                if (!threadsMap.containsKey(threadId)) {
                    threadsMap[threadId] = SmsThread(
                        threadId = threadId,
                        address = cursor.getString(addressIndex) ?: "",
                        snippet = cursor.getString(bodyIndex) ?: "",
                        date = cursor.getLong(dateIndex),
                        messageCount = 0, // Will be updated later if needed
                        read = cursor.getInt(readIndex) == 1
                    )
                }
            }
        }

        return threadsMap.values.sortedByDescending { it.date }
    }

    fun getMessagesInThread(threadId: Long): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ
        )
        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())

        context.contentResolver.query(uri, projection, selection, selectionArgs, "${Telephony.Sms.DATE} ASC")?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadIdIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val readIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)

            while (cursor.moveToNext()) {
                messages.add(
                    SmsMessage(
                        id = cursor.getLong(idIndex),
                        threadId = cursor.getLong(threadIdIndex),
                        address = cursor.getString(addressIndex) ?: "",
                        body = cursor.getString(bodyIndex) ?: "",
                        date = cursor.getLong(dateIndex),
                        type = cursor.getInt(typeIndex),
                        read = cursor.getInt(readIndex) == 1
                    )
                )
            }
        }

        return messages
    }

    fun sendSms(phoneNumber: String, message: String): Boolean {
        return try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)

            // Save sent message to ContentProvider
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, phoneNumber)
                put(Telephony.Sms.BODY, message)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                put(Telephony.Sms.SEEN, 1)
            }

            val uri = context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
            Log.d("SmsRepository", "Sent message saved to ContentProvider: $uri")

            true
        } catch (e: Exception) {
            Log.e("SmsRepository", "Failed to send SMS", e)
            e.printStackTrace()
            false
        }
    }

    fun deleteMessage(messageId: Long): Boolean {
        return try {
            val uri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, messageId.toString())
            val deletedRows = context.contentResolver.delete(uri, null, null)
            Log.d("SmsRepository", "Deleted message: $messageId, rows affected: $deletedRows")
            deletedRows > 0
        } catch (e: Exception) {
            Log.e("SmsRepository", "Failed to delete message", e)
            e.printStackTrace()
            false
        }
    }
}
