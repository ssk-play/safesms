package ssk.safesms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MmsReceiver", "WAP Push received: ${intent.action}")
        // MMS/WAP Push 처리 로직은 추후 구현
    }
}
