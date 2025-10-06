package ssk.safesms

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ssk.safesms.ui.conversation.ConversationViewModel
import ssk.safesms.ui.home.HomeViewModel
import ssk.safesms.ui.screens.ConversationScreen
import ssk.safesms.ui.screens.SmsListScreen
import ssk.safesms.ui.theme.SafeSmsTheme

class MainActivityCompose : ComponentActivity() {

    // RoleManager를 사용한 기본 SMS 앱 요청 (Android 10+)
    private val roleManagerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                Toast.makeText(this, "SafeSms가 기본 SMS 앱으로 설정되었습니다", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "기본 SMS 앱으로 설정되지 않았습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SafeSmsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SafeSmsApp(
                        onRequestDefaultSmsApp = { requestDefaultSmsApp() }
                    )
                }
            }
        }
    }

    private fun requestDefaultSmsApp() {
        Log.d("MainActivityCompose", "Requesting default SMS app")
        Log.d("MainActivityCompose", "Current package: $packageName")
        Log.d("MainActivityCompose", "Current default SMS package: ${Telephony.Sms.getDefaultSmsPackage(this)}")
        Log.d("MainActivityCompose", "Android version: ${Build.VERSION.SDK_INT}")

        try {
            // Android 10 (API 29) 이상: RoleManager 사용
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager

                if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                        Log.d("MainActivityCompose", "Using RoleManager (Android 10+)")
                        roleManagerLauncher.launch(intent)
                    } else {
                        Log.d("MainActivityCompose", "Already default SMS app")
                        Toast.makeText(this, "이미 기본 SMS 앱입니다", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("MainActivityCompose", "SMS role not available")
                    fallbackToSettings()
                }
            }
            // Android 4.4 ~ 9: ACTION_CHANGE_DEFAULT 사용
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

    private fun fallbackToSettings() {
        try {
            Log.d("MainActivityCompose", "Opening system settings as fallback")
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "설정에서 '기본 앱 > SMS 앱'을 선택하여 SafeSms를 설정해주세요", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("MainActivityCompose", "Failed to open settings", e)
            Toast.makeText(this, "설정을 열 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun SafeSmsApp(
    onRequestDefaultSmsApp: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var showDefaultSmsDialog by remember { mutableStateOf(false) }
    var permissionsGranted by remember { mutableStateOf(false) }

    // 필요한 권한 목록
    val requiredPermissions = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_CONTACTS
    )

    // 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionsGranted = allGranted
        if (!allGranted) {
            Toast.makeText(context, "SMS 기능을 사용하려면 권한이 필요합니다", Toast.LENGTH_LONG).show()
        }
    }

    // 앱 시작 시 권한 확인 및 요청
    LaunchedEffect(Unit) {
        val missingPermissions = requiredPermissions.filter {
            context.checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            android.util.Log.d("SafeSmsApp", "All permissions already granted")
            permissionsGranted = true
        } else {
            android.util.Log.d("SafeSmsApp", "Requesting ${missingPermissions.size} permissions")
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    // 권한 허용 후 기본 SMS 앱인지 확인 (한 번만)
    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            val prefs = context.getSharedPreferences("safesms_prefs", Context.MODE_PRIVATE)
            val alreadyAsked = prefs.getBoolean("default_sms_asked", false)

            if (!alreadyAsked) {
                val isDefaultSmsApp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+: RoleManager 사용
                    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                    val isDefault = roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
                    android.util.Log.d("SafeSmsApp", "Using RoleManager - isRoleHeld: $isDefault")
                    isDefault
                } else {
                    // Android 9 이하: Telephony API 사용
                    val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context)
                    val isDefault = defaultSmsPackage == context.packageName
                    android.util.Log.d("SafeSmsApp", "Using Telephony - default: $defaultSmsPackage, ours: ${context.packageName}")
                    isDefault
                }

                if (!isDefaultSmsApp) {
                    showDefaultSmsDialog = true
                } else {
                    // 이미 기본 앱이면 다시 묻지 않음
                    prefs.edit().putBoolean("default_sms_asked", true).apply()
                }
            }
        }
    }

    // 기본 SMS 앱 설정 다이얼로그
    if (showDefaultSmsDialog) {
        AlertDialog(
            onDismissRequest = {
                showDefaultSmsDialog = false
                // 나중에 다시 묻지 않도록 표시
                val prefs = context.getSharedPreferences("safesms_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("default_sms_asked", true).apply()
            },
            title = { Text("기본 SMS 앱 설정") },
            text = { Text("SafeSms를 기본 SMS 앱으로 설정하시겠습니까?\n\n기본 SMS 앱으로 설정하면 모든 SMS를 이 앱에서 받을 수 있습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    android.util.Log.d("SafeSmsApp", "User clicked 설정 button")
                    showDefaultSmsDialog = false
                    // 나중에 다시 묻지 않도록 표시
                    val prefs = context.getSharedPreferences("safesms_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("default_sms_asked", true).apply()
                    onRequestDefaultSmsApp()
                }) {
                    Text("설정")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDefaultSmsDialog = false
                    // 나중에 다시 묻지 않도록 표시
                    val prefs = context.getSharedPreferences("safesms_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("default_sms_asked", true).apply()
                }) {
                    Text("나중에")
                }
            }
        )
    }

    // 권한이 허용될 때까지 로딩 표시
    if (!permissionsGranted) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("권한 확인 중...")
            }
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = "sms_list"
    ) {
        composable("sms_list") {
            val viewModel: HomeViewModel = viewModel()
            SmsListScreen(
                viewModel = viewModel,
                onThreadClick = { thread ->
                    navController.navigate("conversation/${thread.threadId}/${thread.address}")
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
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
