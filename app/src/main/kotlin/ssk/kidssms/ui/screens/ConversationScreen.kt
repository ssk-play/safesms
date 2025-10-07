package ssk.kidssms.ui.screens

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ssk.kidssms.data.model.SmsMessage
import ssk.kidssms.notification.SmsNotificationManager
import ssk.kidssms.receiver.SmsReceiver
import ssk.kidssms.ui.conversation.ConversationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    threadId: Long,
    address: String,
    viewModel: ConversationViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val messages by viewModel.messages.observeAsState(emptyList())
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var selectedMessage by remember { mutableStateOf<SmsMessage?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()
    var showTextSelectionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(threadId, address) {
        viewModel.loadMessages(threadId)
        // Set current conversation
        SmsNotificationManager.setCurrentConversation(address)
    }

    // Reset current conversation when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            SmsNotificationManager.setCurrentConversation(null)
        }
    }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Detect SMS reception
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == SmsReceiver.ACTION_SMS_RECEIVED) {
                    android.util.Log.d("ConversationScreen", "SMS received, reloading messages with delay")
                    // Wait for SMS to be fully saved to ContentProvider
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(500)
                        viewModel.loadMessages(threadId)
                    }
                }
            }
        }
        val filter = IntentFilter(SmsReceiver.ACTION_SMS_RECEIVED)

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            android.util.Log.e("ConversationScreen", "Failed to register receiver", e)
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                android.util.Log.e("ConversationScreen", "Failed to unregister receiver", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(address) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message") },
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(address, messageText)
                                messageText = ""
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send")
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages) { message ->
                MessageItem(
                    message = message,
                    onLongClick = {
                        selectedMessage = message
                        showBottomSheet = true
                    }
                )
            }
        }

        if (showBottomSheet && selectedMessage != null) {
            MessageOptionsBottomSheet(
                message = selectedMessage!!,
                sheetState = bottomSheetState,
                onDismiss = {
                    showBottomSheet = false
                    selectedMessage = null
                },
                onCopyText = { msg ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("SMS Message", msg.body)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                    showBottomSheet = false
                },
                onSelectText = { msg ->
                    showBottomSheet = false
                    showTextSelectionDialog = true
                },
                onForward = { msg ->
                    // TODO: Implement forward
                    Toast.makeText(context, "Forward not yet implemented", Toast.LENGTH_SHORT).show()
                    showBottomSheet = false
                },
                onDelete = { msg ->
                    // TODO: Implement delete
                    Toast.makeText(context, "Delete not yet implemented", Toast.LENGTH_SHORT).show()
                    showBottomSheet = false
                }
            )
        }

        if (showTextSelectionDialog && selectedMessage != null) {
            TextSelectionDialog(
                message = selectedMessage!!,
                onDismiss = {
                    showTextSelectionDialog = false
                    selectedMessage = null
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: SmsMessage,
    onLongClick: () -> Unit
) {
    val isSent = message.type == SmsMessage.TYPE_SENT

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isSent) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSent) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .combinedClickable(
                        onClick = { /* No action on regular click */ },
                        onLongClick = onLongClick,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
            ) {
                // SAFETY FEATURE: Links are NOT clickable
                // Compose Text does not automatically detect or activate links
                // This keeps children safe from accidental web browsing
                Text(
                    text = message.body,
                    modifier = Modifier.padding(12.dp),
                    color = if (isSent) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.date)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageOptionsBottomSheet(
    message: SmsMessage,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onCopyText: (SmsMessage) -> Unit,
    onSelectText: (SmsMessage) -> Unit,
    onForward: (SmsMessage) -> Unit,
    onDelete: (SmsMessage) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Message Options",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider()

            MessageOptionItem(
                text = "텍스트 선택",
                onClick = { onSelectText(message) }
            )

            MessageOptionItem(
                text = "텍스트 복사",
                onClick = { onCopyText(message) }
            )

            MessageOptionItem(
                text = "전달",
                onClick = { onForward(message) }
            )

            MessageOptionItem(
                text = "삭제",
                onClick = { onDelete(message) }
            )
        }
    }
}

@Composable
fun MessageOptionItem(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
    }
}

@Composable
fun TextSelectionDialog(
    message: SmsMessage,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Select Text",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                SelectionContainer {
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
