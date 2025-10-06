package ssk.safesms.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ssk.safesms.data.model.SmsThread
import ssk.safesms.receiver.SmsReceiver
import ssk.safesms.ui.home.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsListScreen(
    viewModel: HomeViewModel = viewModel(),
    onThreadClick: (SmsThread) -> Unit
) {
    val threads by viewModel.threads.observeAsState(emptyList())
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initial load
    LaunchedEffect(Unit) {
        viewModel.loadThreads()
    }

    // Lifecycle aware - refresh on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadThreads()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val coroutineScope = rememberCoroutineScope()

    // Detect SMS reception
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                android.util.Log.d("SmsListScreen", "Broadcast received: ${intent?.action}")
                if (intent?.action == SmsReceiver.ACTION_SMS_RECEIVED) {
                    android.util.Log.d("SmsListScreen", "Loading threads with delay...")
                    // Wait for SMS to be fully saved to ContentProvider
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(500)
                        viewModel.loadThreads()
                    }
                }
            }
        }
        val filter = IntentFilter(SmsReceiver.ACTION_SMS_RECEIVED)

        try {
            // Android 13+ requires RECEIVER_NOT_EXPORTED
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            android.util.Log.d("SmsListScreen", "Receiver registered successfully")
        } catch (e: Exception) {
            android.util.Log.e("SmsListScreen", "Failed to register receiver", e)
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
                android.util.Log.d("SmsListScreen", "Receiver unregistered")
            } catch (e: Exception) {
                android.util.Log.e("SmsListScreen", "Failed to unregister receiver", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kids SMS") },
                actions = {
                    IconButton(onClick = { viewModel.loadThreads() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (threads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No messages")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(threads) { thread ->
                    SmsThreadItem(
                        thread = thread,
                        onClick = { onThreadClick(thread) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun SmsThreadItem(
    thread: SmsThread,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = thread.address,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                // SAFETY FEATURE: Links in message preview are NOT clickable
                Text(
                    text = thread.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatDate(thread.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diff = now.time - date.time

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        else -> {
            SimpleDateFormat("MM/dd", Locale.getDefault()).format(date)
        }
    }
}

@Composable
fun <T> androidx.lifecycle.LiveData<T>.observeAsState(initial: T): State<T> {
    val state = remember { mutableStateOf(initial) }
    DisposableEffect(this) {
        val observer = androidx.lifecycle.Observer<T> { state.value = it }
        observeForever(observer)
        onDispose { removeObserver(observer) }
    }
    return state
}
