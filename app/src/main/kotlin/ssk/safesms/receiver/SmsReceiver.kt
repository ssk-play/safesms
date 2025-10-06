package ssk.safesms.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import android.widget.Toast
import ssk.safesms.notification.SmsNotificationManager

/**
 * SMS 수신 BroadcastReceiver (기본 SMS 앱 전용)
 * SMS_DELIVER만 처리 - 직접 ContentProvider에 저장
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            Log.w("SmsReceiver", "Unexpected action: ${intent.action}")
            return
        }

        Log.d("SmsReceiver", "SMS_DELIVER received")

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages == null || messages.isEmpty()) {
            Log.w("SmsReceiver", "No messages in intent")
            return
        }

        // Notification Manager 초기화
        val notificationManager = SmsNotificationManager(context)

        for (message in messages) {
            val address = message.originatingAddress ?: "Unknown"
            val body = message.messageBody ?: ""
            val timestamp = message.timestampMillis

            Log.d("SmsReceiver", "Saving SMS from $address: $body")

            try {
                // ContentProvider에 저장
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, address)
                    put(Telephony.Sms.BODY, body)
                    put(Telephony.Sms.DATE, timestamp)
                    put(Telephony.Sms.DATE_SENT, timestamp)
                    put(Telephony.Sms.READ, 0)
                    put(Telephony.Sms.SEEN, 0)
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                }

                val uri = context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
                Log.d("SmsReceiver", "SMS saved to: $uri")

                // threadId 추출
                val threadId = uri?.lastPathSegment?.toLongOrNull() ?: -1L

                // Notification 표시 (조건 확인 후)
                notificationManager.showMessageNotification(
                    address = address,
                    message = body,
                    threadId = threadId,
                    unreadCount = 1
                )

            } catch (e: Exception) {
                Log.e("SmsReceiver", "Failed to save SMS", e)
                Toast.makeText(context, "SMS 저장 실패: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // UI 업데이트 알림
        val updateIntent = Intent(ACTION_SMS_RECEIVED).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(updateIntent)
        Log.d("SmsReceiver", "Update broadcast sent")
    }

    companion object {
        const val ACTION_SMS_RECEIVED = "ssk.safesms.SMS_RECEIVED"
    }
}
