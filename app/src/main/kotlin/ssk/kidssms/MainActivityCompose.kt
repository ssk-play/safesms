package ssk.kidssms

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ssk.kidssms.notification.SmsNotificationManager
import ssk.kidssms.ui.conversation.ConversationViewModel
import ssk.kidssms.ui.home.HomeViewModel
import ssk.kidssms.ui.screens.ConversationScreen
import ssk.kidssms.ui.screens.SmsListScreen
import ssk.kidssms.ui.theme.KidsSMSTheme
import ssk.kidssms.ui.ComposeSmsScreen
import android.provider.ContactsContract
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ssk.kidssms.data.repository.SmsRepository

class MainActivityCompose : ComponentActivity() {

    // Request default SMS app using RoleManager (Android 10+)
    private val roleManagerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            val isDefaultSmsApp = roleManager.isRoleHeld(RoleManager.ROLE_SMS)

            if (isDefaultSmsApp) {
                Toast.makeText(this, "KidsSms is now the default SMS app", Toast.LENGTH_SHORT).show()
                Log.d("MainActivityCompose", "Default SMS app set successfully")
            } else {
                Toast.makeText(this, "Not set as default SMS app", Toast.LENGTH_SHORT).show()
                Log.d("MainActivityCompose", "User did not set as default SMS app")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KidsSMSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KidsSmsApp(
                        onRequestDefaultSmsApp = { requestDefaultSmsApp() },
                        onOpenSystemSettings = { openSystemSettings() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SmsNotificationManager.setAppForeground(true)
    }

    override fun onPause() {
        super.onPause()
        SmsNotificationManager.setAppForeground(false)
    }

    private fun requestDefaultSmsApp() {
        Log.d("MainActivityCompose", "Requesting default SMS app")
        Log.d("MainActivityCompose", "Current package: $packageName")
        Log.d("MainActivityCompose", "Current default SMS package: ${Telephony.Sms.getDefaultSmsPackage(this)}")
        Log.d("MainActivityCompose", "Android version: ${Build.VERSION.SDK_INT}")

        try {
            // Android 10 (API 29) and above: Use RoleManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager

                if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                        Log.d("MainActivityCompose", "Using RoleManager (Android 10+)")
                        roleManagerLauncher.launch(intent)
                    } else {
                        Log.d("MainActivityCompose", "Already default SMS app")
                        Toast.makeText(this, "Already set as default SMS app", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("MainActivityCompose", "SMS role not available")
                    fallbackToSettings()
                }
            }
            // Android 4.4 ~ 9: Use ACTION_CHANGE_DEFAULT
            else {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                    putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                }

                if (intent.resolveActivity(packageManager) != null) {
                    Log.d("MainActivityCompose", "Using ACTION_CHANGE_DEFAULT (Android 4.4-9)")
                    startActivity(intent)
                } else {
                    Log.e("MainActivityCompose", "ACTION_CHANGE_DEFAULT not available")
                    fallbackToSettings()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivityCompose", "Failed to request default SMS app", e)
            fallbackToSettings()
        }
    }

    fun openSystemSettings() {
        try {
            Log.d("MainActivityCompose", "Opening system default apps settings")
            // Navigate directly to default apps settings
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            } else {
                Intent(Settings.ACTION_SETTINGS)
            }
            startActivity(intent)
            Toast.makeText(this, "Please select KidsSms as your SMS app", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("MainActivityCompose", "Failed to open default apps settings, trying general settings", e)
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, "Go to Settings > Default apps > SMS app and select KidsSms", Toast.LENGTH_LONG).show()
            } catch (e2: Exception) {
                Log.e("MainActivityCompose", "Failed to open settings", e2)
                Toast.makeText(this, "Unable to open settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fallbackToSettings() {
        openSystemSettings()
    }
}

// Get phone number from contact URI
private fun getPhoneNumberFromContact(context: Context, contactUri: Uri): String? {
    val cursor = context.contentResolver.query(
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

// Check if this app is the default SMS app
private fun isDefaultSmsApp(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 10+: Use RoleManager
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
        val isDefault = roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
        android.util.Log.d("KidsSmsApp", "RoleManager check - isDefault: $isDefault")
        isDefault
    } else {
        // Android 9 and below: Use Telephony API
        val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context)
        val isDefault = defaultSmsPackage == context.packageName
        android.util.Log.d("KidsSmsApp", "Telephony check - default: $defaultSmsPackage, ours: ${context.packageName}, isDefault: $isDefault")
        isDefault
    }
}

@Composable
fun KidsSmsApp(
    onRequestDefaultSmsApp: () -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var isDefaultSmsAppSet by remember { mutableStateOf(false) }
    var showDefaultSmsDialog by remember { mutableStateOf(false) }
    var permissionsGranted by remember { mutableStateOf(false) }
    var settingClickCount by remember { mutableStateOf(0) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Required permissions list
    val requiredPermissions = buildList {
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.READ_CONTACTS)
        // Android 13+ Notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionsGranted = allGranted
        if (!allGranted) {
            Toast.makeText(context, "Permissions are required to use SMS features", Toast.LENGTH_LONG).show()
        }
    }

    // STEP 1: Check if default SMS app first (Play Store requirement)
    LaunchedEffect(Unit) {
        val isDefault = isDefaultSmsApp(context)
        android.util.Log.d("KidsSmsApp", "Initial check - isDefaultSmsApp: $isDefault")
        isDefaultSmsAppSet = isDefault
        showDefaultSmsDialog = !isDefault
    }

    // STEP 2: After default SMS app is set, request runtime permissions
    LaunchedEffect(isDefaultSmsAppSet) {
        if (isDefaultSmsAppSet) {
            android.util.Log.d("KidsSmsApp", "Default SMS app is set, checking permissions")
            val missingPermissions = requiredPermissions.filter {
                context.checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions.isEmpty()) {
                android.util.Log.d("KidsSmsApp", "All permissions already granted")
                permissionsGranted = true
            } else {
                android.util.Log.d("KidsSmsApp", "Requesting ${missingPermissions.size} permissions")
                permissionLauncher.launch(missingPermissions.toTypedArray())
            }
        }
    }

    // Re-check default SMS app status on resume
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                android.util.Log.d("KidsSmsApp", "onResume - checking status")
                val isDefault = isDefaultSmsApp(context)
                isDefaultSmsAppSet = isDefault
                showDefaultSmsDialog = !isDefault
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Show loading until both default app and permissions are set
    if (!isDefaultSmsAppSet || !permissionsGranted) {
        // Skip showing loading if we're about to show the default SMS dialog
        if (!showDefaultSmsDialog && !permissionsGranted) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Initializing...")
                }
            }
            return
        }
    }

    // Show full-screen guide if not default app
    if (showDefaultSmsDialog) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "KidsSms",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "You must set Kids SMS as your default SMS app\nto use all features.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            settingClickCount++
                            Log.d("KidsSmsApp", "User clicked 설정 button (count: $settingClickCount)")
                            onRequestDefaultSmsApp()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Set as Default SMS App")
                    }

                    // Show direct settings button after 3+ clicks
                    if (settingClickCount >= 3) {
                        OutlinedButton(
                            onClick = {
                                Log.d("KidsSmsApp", "User clicked direct settings button")
                                onOpenSystemSettings()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open System Settings")
                        }
                    }
                }
            }
        }
    } else {
        // Render NavHost only when default app
        NavHost(
            navController = navController,
            startDestination = "sms_list",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("sms_list") {
                val viewModel: HomeViewModel = viewModel()
                SmsListScreen(
                    viewModel = viewModel,
                    onThreadClick = { thread ->
                        navController.navigate("conversation/${thread.threadId}/${thread.address}")
                    },
                    onNewMessageClick = {
                        navController.navigate("compose_new")
                    }
                )
            }

            composable("compose_new") {
                var recipient by remember { mutableStateOf("") }
                val coroutineScope = rememberCoroutineScope()

                val contactPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == ComponentActivity.RESULT_OK) {
                        result.data?.data?.let { contactUri ->
                            val phoneNumber = getPhoneNumberFromContact(context, contactUri)
                            if (phoneNumber != null) {
                                recipient = phoneNumber
                            }
                        }
                    }
                }

                ComposeSmsScreen(
                    recipient = recipient,
                    onRecipientChange = { recipient = it },
                    initialMessage = "",
                    onSend = { recipientValue, message ->
                        coroutineScope.launch {
                            try {
                                val repository = SmsRepository(context)
                                val success = withContext(Dispatchers.IO) {
                                    repository.sendSms(recipientValue, message)
                                }

                                if (success) {
                                    Toast.makeText(context, "Message sent", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("KidsSmsApp", "Failed to send SMS", e)
                                Toast.makeText(context, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onContactPick = {
                        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        contactPickerLauncher.launch(intent)
                    }
                )
            }

            composable(
                route = "conversation/{threadId}/{address}",
                arguments = listOf(
                    navArgument("threadId") { type = NavType.LongType },
                    navArgument("address") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val threadId = backStackEntry.arguments?.getLong("threadId") ?: -1L
                val address = backStackEntry.arguments?.getString("address") ?: ""
                val viewModel: ConversationViewModel = viewModel()

                ConversationScreen(
                    threadId = threadId,
                    address = address,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onForwardMessage = { message ->
                        val encodedMessage = Uri.encode(message)
                        navController.navigate("forward/$encodedMessage")
                    }
                )
            }

            composable(
                route = "forward/{message}",
                arguments = listOf(
                    navArgument("message") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedMessage = backStackEntry.arguments?.getString("message") ?: ""
                val message = Uri.decode(encodedMessage)
                var recipient by remember { mutableStateOf("") }
                val coroutineScope = rememberCoroutineScope()

                val contactPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == ComponentActivity.RESULT_OK) {
                        result.data?.data?.let { contactUri ->
                            val phoneNumber = getPhoneNumberFromContact(context, contactUri)
                            if (phoneNumber != null) {
                                recipient = phoneNumber
                            }
                        }
                    }
                }

                ComposeSmsScreen(
                    recipient = recipient,
                    onRecipientChange = { recipient = it },
                    initialMessage = message,
                    onSend = { recipientValue, messageValue ->
                        coroutineScope.launch {
                            try {
                                val repository = SmsRepository(context)
                                val success = withContext(Dispatchers.IO) {
                                    repository.sendSms(recipientValue, messageValue)
                                }

                                if (success) {
                                    Toast.makeText(context, "Message forwarded", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, "Failed to forward message", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("KidsSmsApp", "Failed to forward SMS", e)
                                Toast.makeText(context, "Failed to forward: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onContactPick = {
                        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        contactPickerLauncher.launch(intent)
                    }
                )
            }
        }
    }
}
