package ssk.kidssms.ui.screens

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Patterns
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ssk.kidssms.data.model.SmsMessage
import ssk.kidssms.data.repository.ContactsRepository
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
    onBackClick: () -> Unit,
    onForwardMessage: (String) -> Unit = {}
) {
    val messages by viewModel.messages.observeAsState(emptyList())
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val contactsRepository = remember { ContactsRepository(context) }
    val displayName = remember(address) {
        contactsRepository.getDisplayName(address)
    }

    var selectedMessage by remember { mutableStateOf<SmsMessage?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()
    var showTextSelectionDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showLinkSelectionDialog by remember { mutableStateOf(false) }
    var linksToShow by remember { mutableStateOf<List<String>>(emptyList()) }

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
                @Suppress("UnspecifiedRegisterReceiverFlag")
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
                title = { Text(displayName) },
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
                    showBottomSheet = false
                    selectedMessage = null
                    onForwardMessage(msg.body)
                },
                onDelete = { msg ->
                    showBottomSheet = false
                    showDeleteConfirmDialog = true
                },
                onCopyLink = { msg ->
                    val links = extractLinks(msg.body)
                    if (links.size == 1) {
                        // Single link: copy directly
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Link", links[0])
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                        showBottomSheet = false
                    } else if (links.size > 1) {
                        // Multiple links: show selection dialog
                        linksToShow = links
                        showBottomSheet = false
                        showLinkSelectionDialog = true
                    }
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

        if (showDeleteConfirmDialog && selectedMessage != null) {
            DeleteConfirmDialog(
                onConfirm = {
                    viewModel.deleteMessage(selectedMessage!!.id)
                    showDeleteConfirmDialog = false
                    selectedMessage = null
                    Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                },
                onDismiss = {
                    showDeleteConfirmDialog = false
                    selectedMessage = null
                }
            )
        }

        if (showLinkSelectionDialog && linksToShow.isNotEmpty()) {
            LinkSelectionDialog(
                links = linksToShow,
                onLinkSelected = { link ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Link", link)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                    showLinkSelectionDialog = false
                    selectedMessage = null
                    linksToShow = emptyList()
                },
                onDismiss = {
                    showLinkSelectionDialog = false
                    selectedMessage = null
                    linksToShow = emptyList()
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
    onDelete: (SmsMessage) -> Unit,
    onCopyLink: (SmsMessage) -> Unit
) {
    val links = remember(message.body) { extractLinks(message.body) }

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

            if (links.isNotEmpty()) {
                MessageOptionItem(
                    text = "링크 복사",
                    onClick = { onCopyLink(message) }
                )
            }

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

@Composable
fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete Message")
        },
        text = {
            Text("Are you sure you want to delete this message?")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LinkSelectionDialog(
    links: List<String>,
    onLinkSelected: (String) -> Unit,
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
                    text = "링크 선택",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                links.forEach { link ->
                    Surface(
                        onClick = { onLinkSelected(link) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = link,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (link != links.last()) {
                        HorizontalDivider()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("취소")
                    }
                }
            }
        }
    }
}

/**
 * Extract URLs from text
 */
fun extractLinks(text: String): List<String> {
    val links = mutableListOf<String>()
    val matcher = Patterns.WEB_URL.matcher(text)
    while (matcher.find()) {
        matcher.group()?.let { links.add(it) }
    }
    return links
}
