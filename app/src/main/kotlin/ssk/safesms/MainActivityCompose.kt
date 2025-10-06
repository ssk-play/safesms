package ssk.safesms

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
                    SafeSmsApp()
                }
            }
        }
    }
}

@Composable
fun SafeSmsApp() {
    val navController = rememberNavController()

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
