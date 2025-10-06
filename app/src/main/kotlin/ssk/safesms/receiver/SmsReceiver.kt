package ssk.safesms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d("SmsReceiver", "SMS received, broadcasting update")

            // SMS 목록 업데이트 알림
            val updateIntent = Intent(ACTION_SMS_RECEIVED)
            context.sendBroadcast(updateIntent)
        }
    }

    companion object {
        const val ACTION_SMS_RECEIVED = "ssk.safesms.SMS_RECEIVED"
    }
}
