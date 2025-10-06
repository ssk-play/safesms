package ssk.safesms.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log

/**
 * Service for Quick Response when receiving phone calls
 */
class HeadlessSmsSendService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("HeadlessSmsSendService", "onStartCommand: ${intent?.action}")

        if (intent?.action == "android.intent.action.RESPOND_VIA_MESSAGE") {
            val message = intent.getStringExtra(Intent.EXTRA_TEXT)
            val recipientUri = intent.data

            val phoneNumber = recipientUri?.schemeSpecificPart
            Log.d("HeadlessSmsSendService", "Sending quick response to $phoneNumber: $message")

            if (phoneNumber != null && message != null) {
                try {
                    val smsManager = SmsManager.getDefault()
                    smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                    Log.d("HeadlessSmsSendService", "Quick response sent successfully")
                } catch (e: Exception) {
                    Log.e("HeadlessSmsSendService", "Failed to send quick response", e)
                }
            }
        }

        stopSelf(startId)
        return START_NOT_STICKY
    }
}
