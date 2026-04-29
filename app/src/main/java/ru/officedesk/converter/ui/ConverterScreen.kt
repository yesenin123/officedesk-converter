package ru.officedesk.converter.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.officedesk.converter.ConverterVM
import ru.officedesk.converter.DocFormat
import ru.officedesk.converter.JobState
import ru.officedesk.converter.Sharing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConverterScreen(vm: ConverterVM, onOpenHistory: () -> Unit, onOpenAbout: () -> Unit) {
    val state by vm.state.collectAsState()
    val input by vm.input.collectAsState()
    val to by vm.to.collectAsState()
    val ctx = LocalContext.current

    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Throwable) {}
            vm.pickFile(uri)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("OFFICEDESK · RU",
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.inkMute)
                        Text("Конвертер документов",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = "Журнал")
                    }
                    IconButton(onClick = onOpenAbout) {
                        Icon(Icons.Default.Info, contentDescription = "О программе")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Palette.paper
                )
            )
        },
        containerColor = Palette.paper,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PaperCard {
                Dropzone(
                    fileName = input?.sourceName,
                    fileSize = input?.sourceSize,
                    fmt = input?.from,
                    onPick = { pickFile.launch(arrayOf(
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/rtf",
                        "application/vnd.oasis.opendocument.text",
                        "text/plain",
                        "text/html",
                        "text/markdown",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/pdf",
                        "*/*",
                    )) },
                    onClear = { vm.reset() }
                )

                Spacer(Modifier.height(12.dp))
                Divider(color = Palette.ruleSoft)
                Spacer(Modifier.height(12.dp))

                Text("В ФОРМАТ", style = MaterialTheme.typography.labelSmall, color = Palette.inkMute)
                Spacer(Modifier.height(6.dp))
                FormatGrid(
                    formats = DocFormat.writable,
                    selected = to,
                    disabled = input?.from,
                    onPick = { vm.setTo(it) }
                )

                Spacer(Modifier.height(16.dp))
                Divider(color = Palette.ruleSoft)
                Spacer(Modifier.height(12.dp))

                ActionRow(
                    state = state,
                    inputReady = input != null && input?.from != to,
                    onStart = { vm.start() },
                    onReset = { vm.reset() },
                    onOpenWith = { uri, mime -> Sharing.openWith(ctx, uri, mime) },
                    onOpenInReader = { uri, mime -> Sharing.openInReader(ctx, uri, mime) },
                    onShare = { uri, mime -> Sharing.share(ctx, uri, mime) },
                )
            }

            // Подсказка про читалку
            if (input != null) {
                ReaderHint(ctx = ctx)
            }

            FooterBadge()
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun PaperCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Color(0xFFFFFAF0),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Palette.ruleSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun Dropzone(
    fileName: String?,
    fileSize: Long?,
    fmt: DocFormat?,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        color = Palette.paper2,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Palette.rule),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = fileName == null) { onPick() }
    ) {
        if (fileName == null) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFFAF0))
                        .border(1.dp, Palette.ruleSoft, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.UploadFile, null, tint = Palette.ochreDeep)
                }
                Spacer(Modifier.height(10.dp))
                Text("Выберите документ",
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text("Word, PDF, TXT и другие",
                    color = Palette.inkMute, fontSize = 12.sp)
            }
        } else {
            Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FormatBadge(fmt ?: DocFormat.DOCX, big = true)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(fileName,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        maxLines = 1)
                    Text(
                        listOfNotNull(
                            fileSize?.let { humanSize(it) },
                            fmt?.displayName
                        ).joinToString(" · "),
                        color = Palette.inkMute,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Удалить")
                }
            }
        }
    }
}

@Composable
fun FormatBadge(fmt: DocFormat, big: Boolean = false) {
    val color = formatColor(fmt)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = if (big) 8.dp else 6.dp, vertical = if (big) 6.dp else 3.dp)
    ) {
        Text(
            fmt.ext.uppercase(),
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (big) 13.sp else 10.sp,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormatGrid(
    formats: List<DocFormat>,
    selected: DocFormat,
    disabled: DocFormat?,
    onPick: (DocFormat) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        formats.forEach { f ->
            val isDisabled = f == disabled
            val isSelected = f == selected && !isDisabled
            Surface(
                color = if (isSelected) Palette.ink else Palette.paper2,
                contentColor = if (isSelected) Color.White else Palette.ink,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, if (isSelected) Palette.ink else Palette.rule
                ),
                modifier = Modifier
                    .clickable(enabled = !isDisabled) { onPick(f) }
                    .alpha(if (isDisabled) 0.35f else 1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(formatColor(f))
                    )
                    Text(f.ext.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    state: JobState,
    inputReady: Boolean,
    onStart: () -> Unit,
    onReset: () -> Unit,
    onOpenWith: (android.net.Uri, String) -> Unit,
    onOpenInReader: (android.net.Uri, String) -> Unit,
    onShare: (android.net.Uri, String) -> Unit,
) {
    when (state) {
        is JobState.Idle -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = Palette.inkMute, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Локально на устройстве",
                        color = Palette.inkMute, fontSize = 12.sp)
                }
                Button(
                    onClick = onStart,
                    enabled = inputReady,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Palette.ink,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Bolt, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Конвертировать")
                }
            }
        }
        is JobState.Loading -> ProgressView("ЗАГРУЗКА", state.pct)
        is JobState.Converting -> ProgressView("КОНВЕРТАЦИЯ", state.pct)
        is JobState.Done -> {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(Palette.moss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Готово",
                            fontFamily = FontFamily.Serif,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold)
                        Text(state.outputName, color = Palette.inkMute, fontSize = 12.sp, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val mime = state.outputFormat.mime
                    if (state.outputFormat in listOf(DocFormat.PDF, DocFormat.TXT, DocFormat.HTML)) {
                        FilledTonalButton(
                            onClick = { onOpenInReader(state.outputUri, mime) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Palette.ochreDeep,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("В читалке")
                        }
                    }
                    OutlinedButton(
                        onClick = { onShare(state.outputUri, mime) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Поделиться")
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onReset) { Text("Ещё файл") }
            }
        }
        is JobState.Error -> {
            Surface(
                color = Color(0xFFF7E5DF),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Palette.stamp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Ошибка", fontWeight = FontWeight.SemiBold)
                        Text(state.message, fontSize = 13.sp, color = Palette.inkSoft)
                    }
                    TextButton(onClick = onReset) { Text("Сбросить") }
                }
            }
        }
    }
}

@Composable
private fun ProgressView(label: String, pct: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Palette.inkSoft)
            Text("$pct%",
                fontFamily = FontFamily.Monospace,
                color = Palette.ochreDeep,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { pct / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Palette.ochreDeep,
            trackColor = Palette.paper2,
        )
    }
}

@Composable
private fun ReaderHint(ctx: android.content.Context) {
    val installed = remember { Sharing.isReaderInstalled(ctx) != null }
    Surface(
        color = Palette.paper2,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Palette.ruleSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.MenuBook, null, tint = Palette.ochreDeep)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (installed) "Открывайте результат в OfficeDesk Читалке"
                    else "Хотите читать удобно? Установите OfficeDesk Читалку",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    if (installed) "Кнопка «В читалке» появится после конвертации"
                    else "Совместима с PDF и TXT",
                    color = Palette.inkMute, fontSize = 12.sp
                )
            }
            if (!installed) {
                TextButton(onClick = { Sharing.suggestInstallReader(ctx) }) {
                    Text("Установить")
                }
            }
        }
    }
}

@Composable
private fun FooterBadge() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Shield, null, tint = Palette.inkMute, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text("Файлы не покидают устройство",
            color = Palette.inkMute, fontSize = 11.sp)
    }
}

internal fun humanSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes Б"
    bytes < 1024 * 1024 -> "${bytes / 1024} КБ"
    else -> "%.1f МБ".format(bytes / 1024.0 / 1024.0)
}

internal fun formatColor(f: DocFormat): Color = when (f) {
    DocFormat.DOC, DocFormat.DOCX -> Color(0xFF1A4480)
    DocFormat.RTF -> Color(0xFF5C6B8A)
    DocFormat.ODT -> Color(0xFF1E6091)
    DocFormat.TXT -> Color(0xFF6B6258)
    DocFormat.PDF -> Color(0xFFA8362B)
    DocFormat.HTML -> Color(0xFFC46A3D)
    DocFormat.MD -> Color(0xFF3A3A3A)
    DocFormat.XLSX, DocFormat.XLS -> Color(0xFF216F3D)
}
