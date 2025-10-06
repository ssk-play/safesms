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
import androidx.compose.ui.text.font.FontWeight
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
            val isDefaultSmsApp = roleManager.isRoleHeld(RoleManager.ROLE_SMS)

            if (isDefaultSmsApp) {
                Toast.makeText(this, "SafeSms가 기본 SMS 앱으로 설정되었습니다", Toast.LENGTH_SHORT).show()
                Log.d("MainActivityCompose", "Default SMS app set successfully")
            } else {
                Toast.makeText(this, "기본 SMS 앱으로 설정되지 않았습니다", Toast.LENGTH_SHORT).show()
                Log.d("MainActivityCompose", "User did not set as default SMS app")
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
                        onRequestDefaultSmsApp = { requestDefaultSmsApp() },
                        onOpenSystemSettings = { openSystemSettings() }
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

    fun openSystemSettings() {
        try {
            Log.d("MainActivityCompose", "Opening system default apps settings")
            // 기본 앱 설정 화면으로 직접 이동
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            } else {
                Intent(Settings.ACTION_SETTINGS)
            }
            startActivity(intent)
            Toast.makeText(this, "SMS 앱을 선택하여 SafeSms를 설정해주세요", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("MainActivityCompose", "Failed to open default apps settings, trying general settings", e)
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, "설정에서 '기본 앱 > SMS 앱'을 선택하여 SafeSms를 설정해주세요", Toast.LENGTH_LONG).show()
            } catch (e2: Exception) {
                Log.e("MainActivityCompose", "Failed to open settings", e2)
                Toast.makeText(this, "설정을 열 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fallbackToSettings() {
        openSystemSettings()
    }
}

// 기본 SMS 앱인지 확인
private fun isDefaultSmsApp(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 10+: RoleManager 사용
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
        val isDefault = roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
        android.util.Log.d("SafeSmsApp", "RoleManager check - isDefault: $isDefault")
        isDefault
    } else {
        // Android 9 이하: Telephony API 사용
        val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context)
        val isDefault = defaultSmsPackage == context.packageName
        android.util.Log.d("SafeSmsApp", "Telephony check - default: $defaultSmsPackage, ours: ${context.packageName}, isDefault: $isDefault")
        isDefault
    }
}

@Composable
fun SafeSmsApp(
    onRequestDefaultSmsApp: () -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var showDefaultSmsDialog by remember { mutableStateOf(false) }
    var permissionsGranted by remember { mutableStateOf(false) }
    var settingClickCount by remember { mutableStateOf(0) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

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

    // 권한 허용 후 기본 SMS 앱인지 확인
    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            showDefaultSmsDialog = !isDefaultSmsApp(context)
        }
    }

    // onResume 시 기본 SMS 앱 상태 재확인
    DisposableEffect(lifecycleOwner, permissionsGranted) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && permissionsGranted) {
                android.util.Log.d("SafeSmsApp", "onResume - checking default SMS app status")
                showDefaultSmsDialog = !isDefaultSmsApp(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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

    // 기본 앱이 아니면 전체 화면에 안내 메시지 표시
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
                        text = "SafeSms",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "SafeSms는 기본 SMS 앱 전용입니다",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "이 앱을 사용하려면 기본 SMS 앱으로 설정해주세요",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            settingClickCount++
                            Log.d("SafeSmsApp", "User clicked 설정 button (count: $settingClickCount)")
                            onRequestDefaultSmsApp()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("기본 SMS 앱으로 설정")
                    }

                    // 3회 이상 클릭 시 직접 설정 버튼 표시
                    if (settingClickCount >= 3) {
                        OutlinedButton(
                            onClick = {
                                Log.d("SafeSmsApp", "User clicked 직접 설정 button")
                                onOpenSystemSettings()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("시스템 설정에서 직접 설정")
                        }
                    }
                }
            }
        }
    } else {
        // 기본 앱일 때만 NavHost 렌더링
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
}
