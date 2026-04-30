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

// Матрица конвертации: какие форматы безопасно конвертировать
fun getSupportedOutputFormats(inputFormat: DocFormat): List<DocFormat> = when (inputFormat) {
    DocFormat.DOCX -> listOf(DocFormat.DOCX, DocFormat.DOC, DocFormat.RTF, DocFormat.PDF, DocFormat.HTML, DocFormat.TXT, DocFormat.MD)
    DocFormat.DOC -> listOf(DocFormat.DOC, DocFormat.DOCX, DocFormat.RTF, DocFormat.PDF, DocFormat.HTML, DocFormat.TXT, DocFormat.MD)
    DocFormat.RTF -> listOf(DocFormat.RTF, DocFormat.DOCX, DocFormat.PDF, DocFormat.HTML, DocFormat.TXT, DocFormat.MD)
    DocFormat.ODT -> listOf(DocFormat.ODT, DocFormat.DOCX, DocFormat.PDF, DocFormat.HTML, DocFormat.TXT, DocFormat.MD)
    DocFormat.TXT -> listOf(DocFormat.TXT, DocFormat.DOCX, DocFormat.PDF, DocFormat.HTML, DocFormat.MD)
    DocFormat.MD -> listOf(DocFormat.MD, DocFormat.DOCX, DocFormat.PDF, DocFormat.HTML, DocFormat.TXT)
    DocFormat.HTML -> listOf(DocFormat.HTML, DocFormat.DOCX, DocFormat.PDF, DocFormat.TXT, DocFormat.MD)
    DocFormat.PDF -> listOf(DocFormat.PDF, DocFormat.TXT, DocFormat.HTML, DocFormat.MD)
    DocFormat.XLSX -> listOf(DocFormat.XLSX, DocFormat.XLS, DocFormat.PDF, DocFormat.HTML, DocFormat.TXT)
    DocFormat.XLS -> listOf(DocFormat.XLS, DocFormat.XLSX, DocFormat.PDF, DocFormat.HTML, DocFormat.TXT)
}
@Composable
fun FormatGrid(
    formats: List<DocFormat>,
    selected: DocFormat?,
    disabled: DocFormat?,
    onPick: (DocFormat) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        formats.forEach { fmt ->
            FormatBadge(
                format = fmt,
                selected = fmt == selected,
                disabled = disabled,
                onClick = { onPick(fmt) }
            )
        }
    }
}

@Composable
fun FormatBadge(
    format: DocFormat,
    selected: Boolean,
    disabled: DocFormat?,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) Palette.ink else if (disabled == format) Palette.paper2 else Color.White,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) Palette.ink else Palette.rule),
        modifier = Modifier
            .clickable(enabled = disabled != format) { onClick() }
            .padding(4.dp)
    ) {
        Text(
            format.ext.uppercase(),
            modifier = Modifier.padding(8.dp, 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else Palette.ink
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConverterScreen(vm: ConverterVM, onOpenHistory: () -> Unit, onOpenAbout: () -> Unit) {
    val state by vm.state.collectAsState()
    val input by vm.input.collectAsState()
    val to by vm.to.collectAsState()
    val ctx = LocalContext.current
    var showUnsupportedError by remember { mutableStateOf(false) }
    var errorFormat by remember { mutableStateOf<DocFormat?>(null) }

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

    val supportedFormats = input?.from?.let { getSupportedOutputFormats(it) } ?: emptyList()
    val isFormatSupported = supportedFormats.isNotEmpty()

    // Показ диалога при загрузке неподдерживаемого формата
    LaunchedEffect(input) {
        if (input != null && !isFormatSupported) {
            errorFormat = input?.from
            showUnsupportedError = true
        }
    }

    if (showUnsupportedError && errorFormat != null) {
        UnsupportedFormatDialog(
            format = errorFormat!!,
            onDismiss = {
                showUnsupportedError = false
                vm.reset()
            }
        )
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
            if (input == null || !isFormatSupported) {
                // Экран 1: Приветствие и загрузка
                PaperCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(Modifier.height(20.dp))
                        Text("Добро пожаловать!",
                            fontFamily = FontFamily.Serif,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Загрузите документ для конвертации",
                            color = Palette.inkMute, fontSize = 14.sp)
                        Spacer(Modifier.height(32.dp))

                        Dropzone(
                            fileName = null,
                            fileSize = null,
                            fmt = null,
                            onPick = {
                                pickFile.launch(arrayOf(
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
                                ))
                            },
                            onClear = {}
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }
            } else {
                // Экран 2: Выбор формата после загрузки
                PaperCard {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Palette.ochreDeep),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Файл загружен",
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold)
                                Text(input!!.sourceName, color = Palette.inkMute, fontSize = 12.sp, maxLines = 1)
                            }
                            TextButton(onClick = { vm.reset() }) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Palette.ruleSoft)
                        Spacer(Modifier.height(16.dp))

                        Text("КОНВЕРТИРОВАТЬ В", style = MaterialTheme.typography.labelSmall, color = Palette.inkMute)
                        Spacer(Modifier.height(8.dp))
                        FormatGrid(
                            formats = supportedFormats,
                            selected = to,
                            disabled = null,
                            onPick = { vm.setTo(it) }
                        )

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Palette.ruleSoft)
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
                }
            }

            FooterBadge()
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun UnsupportedFormatDialog(format: DocFormat, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ErrorOutline, null, tint = Palette.stamp, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Формат не поддерживается",
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column {
                Text("Файл: ${format.displayName}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Palette.ink)
                Spacer(Modifier.height(8.dp))
                Text("К сожалению, этот формат нельзя конвертировать в поддерживаемые форматы.",
                    fontSize = 14.sp,
                    color = Palette.inkMute)
                Spacer(Modifier.height(16.dp))
                Text("Поддерживаемые форматы:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Palette.inkSoft)
                Spacer(Modifier.height(4.dp))
                Text("Word (DOCX, DOC, RTF, ODT), PDF, TXT, HTML, MD, Excel (XLS, XLSX)",
                    fontSize = 13.sp,
                    color = Palette.ink)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Palette.ink,
                    contentColor = Color.White
                )
            ) {
                Text("Выбрать другой файл")
            }
        },
        containerColor = Palette.paper,
        textContentColor = Palette.ink,
        titleContentColor = Palette.ink
    )
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
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
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
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(formatColor(fmt!!)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(fmt.ext.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(fileName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(humanSize(fileSize ?: 0), color = Palette.inkMute, fontSize = 12.sp)
                }
                IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, null, tint = Palette.inkMute, modifier = Modifier.size(18.dp))
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
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Palette.moss),
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
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = Palette.ochreDeep,
            trackColor = Palette.paper2,
        )
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
