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
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_CONTACTS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "권한이 허용되었습니다", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "SMS 기능을 사용하려면 권한이 필요합니다", Toast.LENGTH_LONG).show()
        }
    }

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

        // 권한 요청
        permissionLauncher.launch(requiredPermissions)

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

    // 앱 시작 시 기본 SMS 앱인지 확인
    LaunchedEffect(Unit) {
        val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context)
        android.util.Log.d("SafeSmsApp", "Current default SMS package: $defaultSmsPackage")
        android.util.Log.d("SafeSmsApp", "Our package: ${context.packageName}")
        if (defaultSmsPackage != context.packageName) {
            showDefaultSmsDialog = true
        }
    }

    // 기본 SMS 앱 설정 다이얼로그
    if (showDefaultSmsDialog) {
        AlertDialog(
            onDismissRequest = { showDefaultSmsDialog = false },
            title = { Text("기본 SMS 앱 설정") },
            text = { Text("SafeSms를 기본 SMS 앱으로 설정하시겠습니까?\n\n기본 SMS 앱으로 설정하면 모든 SMS를 이 앱에서 받을 수 있습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    android.util.Log.d("SafeSmsApp", "User clicked 설정 button")
                    showDefaultSmsDialog = false
                    onRequestDefaultSmsApp()
                }) {
                    Text("설정")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDefaultSmsDialog = false }) {
                    Text("나중에")
                }
            }
        )
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
