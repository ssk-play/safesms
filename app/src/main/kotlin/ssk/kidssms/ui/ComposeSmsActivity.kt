package ssk.kidssms.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ssk.kidssms.ui.theme.KidsSMSTheme
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ssk.kidssms.data.repository.SmsRepository
import androidx.lifecycle.lifecycleScope

/**
 * Activity used when external apps request SMS sending
 * (e.g., when "Share message" is selected from another app)
 */
@OptIn(ExperimentalMaterial3Api::class)
class ComposeSmsActivity : ComponentActivity() {

    private var recipientAddress: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract recipient address from Intent
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
            KidsSMSTheme {
                var currentRecipient by remember { mutableStateOf(recipientAddress) }

                val contactPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        result.data?.data?.let { contactUri ->
                            val phoneNumber = getPhoneNumberFromContact(contactUri)
                            if (phoneNumber != null) {
                                currentRecipient = phoneNumber
                            }
                        }
                    }
                }

                ComposeSmsScreen(
                    recipient = currentRecipient,
                    onRecipientChange = { currentRecipient = it },
                    initialMessage = initialMessage,
                    onSend = { recipient, message ->
                        sendSms(recipient, message)
                    },
                    onBack = { finish() },
                    onContactPick = {
                        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        contactPickerLauncher.launch(intent)
                    }
                )
            }
        }
    }

    private fun sendSms(recipient: String, message: String) {
        lifecycleScope.launch {
            try {
                val repository = SmsRepository(applicationContext)
                val success = withContext(Dispatchers.IO) {
                    repository.sendSms(recipient, message)
                }

                if (success) {
                    Toast.makeText(this@ComposeSmsActivity, "Message sent", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ComposeSmsActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ComposeSmsActivity", "Failed to send SMS", e)
                Toast.makeText(this@ComposeSmsActivity, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getPhoneNumberFromContact(contactUri: Uri): String? {
        val cursor = contentResolver.query(
            contactUri,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            null,
            null,
            null
        )
        return cursor?.use {
            if (it.moveToFirst()) {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex >= 0) {
                    it.getString(numberIndex)
                } else null
            } else null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeSmsScreen(
    recipient: String,
    onRecipientChange: (String) -> Unit,
    initialMessage: String,
    onSend: (String, String) -> Unit,
    onBack: () -> Unit,
    onContactPick: (() -> Unit)? = null
) {
    var message by remember { mutableStateOf(initialMessage) }
    val recipientReadOnly = recipient.isNotEmpty() && initialMessage.isNotEmpty()

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
                onValueChange = onRecipientChange,
                label = { Text("To") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !recipientReadOnly,
                trailingIcon = if (onContactPick != null && !recipientReadOnly) {
                    {
                        IconButton(onClick = onContactPick) {
                            Icon(Icons.Default.Person, contentDescription = "Pick Contact")
                        }
                    }
                } else null
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
