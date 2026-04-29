package ru.officedesk.converter.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.officedesk.converter.ConverterVM
import ru.officedesk.converter.DocFormat
import ru.officedesk.converter.HistoryItem
import ru.officedesk.converter.Sharing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: ConverterVM, onBack: () -> Unit) {
    val items by vm.history.collectAsState()
    val ctx = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Журнал", fontFamily = FontFamily.Serif) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        TextButton(onClick = { vm.clearHistory() }) {
                            Text("Очистить", color = Palette.stamp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Palette.paper)
            )
        },
        containerColor = Palette.paper,
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HistoryToggleOff, null,
                        tint = Palette.inkMute, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Пока ничего нет",
                        color = Palette.inkMute, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    HistoryRow(
                        item = item,
                        onOpen = {
                            try {
                                val uri = Uri.parse(item.outputUri)
                                val mime = DocFormat.fromExt(item.outputName)?.mime ?: "*/*"
                                Sharing.openWith(ctx, uri, mime)
                            } catch (_: Throwable) {}
                        }
                    )
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: HistoryItem, onOpen: () -> Unit) {
    Surface(
        color = Color(0xFFFFFAF0),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Palette.ruleSoft),
        modifier = Modifier.fillMaxWidth().clickable { onOpen() }
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val from = DocFormat.fromExt("x.${item.from}") ?: DocFormat.DOCX
            val to = DocFormat.fromExt("x.${item.to}") ?: DocFormat.PDF
            FormatBadge(from)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.ArrowForward, null, tint = Palette.inkMute, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            FormatBadge(to)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.outputName,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1, fontSize = 14.sp)
                Text(item.whenStr(), color = Palette.inkMute, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Palette.inkMute)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uri = LocalUriHandler.current
    val ctx = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("О программе", fontFamily = FontFamily.Serif) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Palette.paper)
            )
        },
        containerColor = Palette.paper,
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("OfficeDesk Конвертер", style = MaterialTheme.typography.titleLarge)
            Text("Версия 1.0.0", color = Palette.inkMute, fontSize = 13.sp)

            Surface(color = Color(0xFFFFFAF0),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Palette.ruleSoft),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = Palette.moss)
                        Spacer(Modifier.width(8.dp))
                        Text("Конфиденциально", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Все файлы обрабатываются на устройстве. Ничего не загружается на серверы.",
                        fontSize = 13.sp, color = Palette.inkSoft)
                }
            }

            Text("Поддержка", style = MaterialTheme.typography.titleMedium)
            LinkRow("Политика конфиденциальности", Icons.Default.Policy) {
                uri.openUri("https://officedesk.ru/privacy")
            }
            LinkRow("Пользовательское соглашение", Icons.Default.Description) {
                uri.openUri("https://officedesk.ru/terms")
            }
            LinkRow("Связаться с поддержкой", Icons.Default.Email) {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:support@officedesk.ru")
                    putExtra(Intent.EXTRA_SUBJECT, "OfficeDesk Конвертер")
                }
                ctx.startActivity(intent)
            }
            LinkRow("Открыть Читалку OfficeDesk", Icons.Default.MenuBook) {
                if (Sharing.isReaderInstalled(ctx) != null) {
                    val launch = ctx.packageManager.getLaunchIntentForPackage("ru.officedesk.reader")
                        ?: ctx.packageManager.getLaunchIntentForPackage("ru.officedesk.reader.debug")
                    if (launch != null) ctx.startActivity(launch)
                } else {
                    Sharing.suggestInstallReader(ctx)
                }
            }

            Text("Возрастная категория: 0+", color = Palette.inkMute, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LinkRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        color = Color(0xFFFFFAF0),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Palette.ruleSoft),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Palette.ochreDeep)
            Spacer(Modifier.width(12.dp))
            Text(title, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Palette.inkMute)
        }
    }
}
