package ssk.safesms.data.repository

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import ssk.safesms.data.model.SmsMessage
import ssk.safesms.data.model.SmsThread

class SmsRepository(private val context: Context) {

    fun getAllThreads(): List<SmsThread> {
        val threads = mutableListOf<SmsThread>()
        val uri = Uri.parse("content://sms/conversations?simple=true")
        val projection = arrayOf("thread_id", "address", "snippet", "date", "msg_count", "read")

        context.contentResolver.query(uri, projection, null, null, "date DESC")?.use { cursor ->
            val threadIdIndex = cursor.getColumnIndexOrThrow("thread_id")
            val addressIndex = cursor.getColumnIndexOrThrow("address")
            val snippetIndex = cursor.getColumnIndexOrThrow("snippet")
            val dateIndex = cursor.getColumnIndexOrThrow("date")
            val msgCountIndex = cursor.getColumnIndexOrThrow("msg_count")
            val readIndex = cursor.getColumnIndexOrThrow("read")

            while (cursor.moveToNext()) {
                threads.add(
                    SmsThread(
                        threadId = cursor.getLong(threadIdIndex),
                        address = cursor.getString(addressIndex) ?: "",
                        snippet = cursor.getString(snippetIndex) ?: "",
                        date = cursor.getLong(dateIndex),
                        messageCount = cursor.getInt(msgCountIndex),
                        read = cursor.getInt(readIndex) == 1
                    )
                )
            }
        }

        return threads
    }

    fun getMessagesInThread(threadId: Long): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val uri = Uri.parse("content://sms/")
        val projection = arrayOf("_id", "thread_id", "address", "body", "date", "type", "read")
        val selection = "thread_id = ?"
        val selectionArgs = arrayOf(threadId.toString())

        context.contentResolver.query(uri, projection, selection, selectionArgs, "date ASC")?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow("_id")
            val threadIdIndex = cursor.getColumnIndexOrThrow("thread_id")
            val addressIndex = cursor.getColumnIndexOrThrow("address")
            val bodyIndex = cursor.getColumnIndexOrThrow("body")
            val dateIndex = cursor.getColumnIndexOrThrow("date")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            val readIndex = cursor.getColumnIndexOrThrow("read")

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
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
