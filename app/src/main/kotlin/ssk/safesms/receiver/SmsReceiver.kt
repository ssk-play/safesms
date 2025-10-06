package ssk.safesms.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import android.widget.Toast

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SmsReceiver", "onReceive called with action: ${intent.action}")

        when (intent.action) {
            Telephony.Sms.Intents.SMS_DELIVER_ACTION -> {
                // 기본 SMS 앱일 때: 직접 ContentProvider에 저장해야 함
                Log.d("SmsReceiver", "SMS_DELIVER received (default SMS app)")

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages != null && messages.isNotEmpty()) {
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

                            Toast.makeText(context, "SafeSms: 새 메시지 수신 ($address)", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.e("SmsReceiver", "Failed to save SMS", e)
                            Toast.makeText(context, "SMS 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // UI 업데이트 알림
                    val updateIntent = Intent(ACTION_SMS_RECEIVED).apply {
                        setPackage(context.packageName)
                    }
                    context.sendBroadcast(updateIntent)
                    Log.d("SmsReceiver", "Update broadcast sent")
                }
            }

            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                // 기본 SMS 앱이 아닐 때: 시스템이 자동으로 저장함
                Log.d("SmsReceiver", "SMS_RECEIVED received (not default SMS app)")
                Toast.makeText(context, "SafeSms: 새 메시지 수신", Toast.LENGTH_SHORT).show()

                // UI 업데이트 알림
                val updateIntent = Intent(ACTION_SMS_RECEIVED).apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(updateIntent)
                Log.d("SmsReceiver", "Update broadcast sent")
            }
        }
    }

    companion object {
        const val ACTION_SMS_RECEIVED = "ssk.safesms.SMS_RECEIVED"
    }
}
