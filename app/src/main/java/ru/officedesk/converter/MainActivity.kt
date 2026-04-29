package ru.officedesk.converter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import ru.officedesk.converter.ui.AboutScreen
import ru.officedesk.converter.ui.ConverterScreen
import ru.officedesk.converter.ui.HistoryScreen
import ru.officedesk.converter.ui.OfficeDeskTheme

class MainActivity : ComponentActivity() {

    private val vm: ConverterVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Принимаем «Открыть с помощью…»
        handleIncoming(intent)

        setContent {
            OfficeDeskTheme {
                var screen by remember { mutableStateOf("main") }
                when (screen) {
                    "main" -> ConverterScreen(
                        vm = vm,
                        onOpenHistory = { screen = "history" },
                        onOpenAbout = { screen = "about" },
                    )
                    "history" -> HistoryScreen(vm) { screen = "main" }
                    "about" -> AboutScreen { screen = "main" }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncoming(intent)
    }

    private fun handleIncoming(intent: Intent?) {
        val uri = intent?.data ?: return
        if (intent.action == Intent.ACTION_VIEW) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Throwable) {}
            vm.pickFile(uri)
        }
    }
}
