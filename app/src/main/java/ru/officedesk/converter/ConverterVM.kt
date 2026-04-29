package ru.officedesk.converter

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.officedesk.converter.engine.ConvertEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConverterVM(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<JobState>(JobState.Idle)
    val state: StateFlow<JobState> = _state.asStateFlow()

    private val _input = MutableStateFlow<JobInput?>(null)
    val input: StateFlow<JobInput?> = _input.asStateFlow()

    private val _to = MutableStateFlow(DocFormat.PDF)
    val to: StateFlow<DocFormat> = _to.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    init {
        _history.value = HistoryStore.load(getApplication())
    }

    fun pickFile(uri: Uri) {
        val ctx: Context = getApplication()
        val (name, size) = queryFileMeta(ctx, uri)
        val from = DocFormat.fromExt(name) ?: DocFormat.DOCX
        _input.value = JobInput(uri, name, size, from, _to.value)
        _state.value = JobState.Idle
    }

    fun setTo(fmt: DocFormat) {
        _to.value = fmt
        _input.value = _input.value?.copy(to = fmt)
    }

    fun reset() {
        _input.value = null
        _state.value = JobState.Idle
    }

    fun start() {
        val job = _input.value ?: return
        if (job.from == job.to) {
            _state.value = JobState.Error("Исходный и целевой формат совпадают")
            return
        }
        viewModelScope.launch {
            ConvertEngine.run(getApplication(), job).collect { st ->
                _state.value = st
                if (st is JobState.Done) {
                    val item = HistoryItem(
                        timestamp = System.currentTimeMillis(),
                        sourceName = job.sourceName,
                        from = job.from.ext,
                        to = job.to.ext,
                        outputUri = st.outputUri.toString(),
                        outputName = st.outputName,
                    )
                    val updated = (listOf(item) + _history.value).take(50)
                    _history.value = updated
                    HistoryStore.save(getApplication(), updated)
                }
            }
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
        HistoryStore.save(getApplication(), emptyList())
    }

    private fun queryFileMeta(ctx: Context, uri: Uri): Pair<String, Long> {
        var name = "document"
        var size = 0L
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val si = c.getColumnIndex(OpenableColumns.SIZE)
                if (ni >= 0) name = c.getString(ni) ?: name
                if (si >= 0) size = c.getLong(si)
            }
        }
        return name to size
    }
}

data class HistoryItem(
    val timestamp: Long,
    val sourceName: String,
    val from: String,
    val to: String,
    val outputUri: String,
    val outputName: String,
) {
    fun whenStr(): String = SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(timestamp))
}
