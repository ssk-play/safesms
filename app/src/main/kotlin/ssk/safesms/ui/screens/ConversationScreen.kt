package ssk.safesms.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ssk.safesms.data.model.SmsMessage
import ssk.safesms.ui.conversation.ConversationViewModel
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

    LaunchedEffect(threadId) {
        viewModel.loadMessages(threadId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(address) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
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
                        placeholder = { Text("메시지 입력") },
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
                        Icon(Icons.AutoMirrored.Filled.Send, "전송")
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
                MessageItem(message = message)
            }
        }
    }
}

@Composable
fun MessageItem(message: SmsMessage) {
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
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
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
