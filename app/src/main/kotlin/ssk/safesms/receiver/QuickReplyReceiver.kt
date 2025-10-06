package ssk.safesms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.RemoteInput
import ssk.safesms.notification.SmsNotificationManager

/**
 * Handles Quick Reply from Notification
 */
class QuickReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SmsNotificationManager.ACTION_REPLY) {
            return
        }

        val address = intent.getStringExtra(SmsNotificationManager.EXTRA_ADDRESS)
        val threadId = intent.getLongExtra(SmsNotificationManager.EXTRA_THREAD_ID, -1L)

        // Extract reply text from RemoteInput
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(SmsNotificationManager.KEY_TEXT_REPLY)?.toString()

        if (address == null || replyText.isNullOrBlank()) {
            Log.w("QuickReplyReceiver", "Invalid reply: address=$address, text=$replyText")
            return
        }

        Log.d("QuickReplyReceiver", "Sending quick reply to $address: $replyText")

        try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            smsManager.sendTextMessage(address, null, replyText, null, null)
            Log.d("QuickReplyReceiver", "Quick reply sent successfully")

            // Remove notification
            val notificationManager = SmsNotificationManager(context)
            notificationManager.cancelNotification(address)

        } catch (e: Exception) {
            Log.e("QuickReplyReceiver", "Failed to send quick reply", e)
            Toast.makeText(context, "Failed to send reply: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
