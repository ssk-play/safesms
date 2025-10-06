package ssk.safesms.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ssk.safesms.ui.theme.SafeSmsTheme
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast

/**
 * 외부 앱에서 SMS 전송 요청 시 사용되는 Activity
 * (예: 다른 앱에서 "메시지 공유" 선택 시)
 */
@OptIn(ExperimentalMaterial3Api::class)
class ComposeSmsActivity : ComponentActivity() {

    private var recipientAddress: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intent에서 수신자 주소 추출
        recipientAddress = when (intent?.action) {
            Intent.ACTION_SENDTO, Intent.ACTION_VIEW -> {
                intent.data?.schemeSpecificPart ?: ""
            }
            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: ""
            }
            else -> ""
        }

        val initialMessage = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: ""

        Log.d("ComposeSmsActivity", "Recipient: $recipientAddress, Message: $initialMessage")

        setContent {
            SafeSmsTheme {
                ComposeSmsScreen(
                    initialRecipient = recipientAddress,
                    initialMessage = initialMessage,
                    onSend = { recipient, message ->
                        sendSms(recipient, message)
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    private fun sendSms(recipient: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(recipient, null, message, null, null)
            Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Log.e("ComposeSmsActivity", "Failed to send SMS", e)
            Toast.makeText(this, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeSmsScreen(
    initialRecipient: String,
    initialMessage: String,
    onSend: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var recipient by remember { mutableStateOf(initialRecipient) }
    var message by remember { mutableStateOf(initialMessage) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Message") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (recipient.isNotBlank() && message.isNotBlank()) {
                                onSend(recipient, message)
                            }
                        },
                        enabled = recipient.isNotBlank() && message.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it },
                label = { Text("To") },
                modifier = Modifier.fillMaxWidth(),
                enabled = initialRecipient.isEmpty()
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                maxLines = 10
            )
        }
    }
}
