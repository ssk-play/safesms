package ssk.safesms.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import ssk.safesms.MainActivityCompose
import ssk.safesms.R
import ssk.safesms.receiver.QuickReplyReceiver

/**
 * Advanced SMS Notification Manager
 * - Unread message count
 * - Per-conversation notification channels
 * - Quick Reply
 * - Heads-up notification
 */
class SmsNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "sms_messages"
        private const val CHANNEL_NAME = "SMS Messages"
        private const val CHANNEL_DESCRIPTION = "New SMS message notifications"

        const val KEY_TEXT_REPLY = "key_text_reply"
        const val ACTION_REPLY = "ssk.safesms.ACTION_REPLY"
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_THREAD_ID = "extra_thread_id"

        private var currentConversationAddress: String? = null
        private var isAppInForeground = false

        fun setCurrentConversation(address: String?) {
            currentConversationAddress = address
        }

        fun setAppForeground(inForeground: Boolean) {
            isAppInForeground = inForeground
        }

        fun shouldShowNotification(messageAddress: String): Boolean {
            // Show notification if in background or viewing a different conversation
            return !isAppInForeground || currentConversationAddress != messageAddress
        }
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showMessageNotification(
        address: String,
        message: String,
        threadId: Long,
        unreadCount: Int = 1
    ) {
        if (!shouldShowNotification(address)) {
            return
        }

        val notification = buildNotification(address, message, threadId, unreadCount)
        notificationManager.notify(address.hashCode(), notification)
    }

    private fun buildNotification(
        address: String,
        message: String,
        threadId: Long,
        unreadCount: Int
    ): Notification {
        // Intent to open conversation
        val openIntent = Intent(context, MainActivityCompose::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("threadId", threadId)
            putExtra("address", address)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            address.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Quick Reply RemoteInput
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply")
            .build()

        // Quick Reply Intent
        val replyIntent = Intent(context, QuickReplyReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra(EXTRA_ADDRESS, address)
            putExtra(EXTRA_THREAD_ID, threadId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            address.hashCode() + 1,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        // Person (sender)
        val sender = Person.Builder()
            .setName(address)
            .setKey(address)
            .build()

        // MessagingStyle
        val messagingStyle = NotificationCompat.MessagingStyle(
            Person.Builder().setName("Me").build()
        )
            .setConversationTitle(address)
            .addMessage(message, System.currentTimeMillis(), sender)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(messagingStyle)
            .setContentIntent(openPendingIntent)
            .addAction(replyAction)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setNumber(unreadCount)
            .build()
    }

    fun cancelNotification(address: String) {
        notificationManager.cancel(address.hashCode())
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
