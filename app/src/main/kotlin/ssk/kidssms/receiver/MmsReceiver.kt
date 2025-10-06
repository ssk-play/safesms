package ssk.kidssms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MmsReceiver", "WAP Push received: ${intent.action}")
        // MMS/WAP Push handling logic to be implemented later
    }
}
