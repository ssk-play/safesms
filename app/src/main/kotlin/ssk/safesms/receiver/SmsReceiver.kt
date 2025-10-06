package ssk.safesms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import android.widget.Toast

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SmsReceiver", "onReceive called with action: ${intent.action}")

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d("SmsReceiver", "SMS received, broadcasting update")
            Toast.makeText(context, "SafeSms: 새 메시지 수신", Toast.LENGTH_SHORT).show()

            // SMS 목록 업데이트 알림 (explicit broadcast for Android 8.0+)
            val updateIntent = Intent(ACTION_SMS_RECEIVED).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(updateIntent)
            Log.d("SmsReceiver", "Update broadcast sent")
        }
    }

    companion object {
        const val ACTION_SMS_RECEIVED = "ssk.safesms.SMS_RECEIVED"
    }
}
