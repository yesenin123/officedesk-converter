package ru.officedesk.converter

import android.net.Uri

data class JobInput(
    val sourceUri: Uri,
    val sourceName: String,
    val sourceSize: Long,
    val from: DocFormat,
    val to: DocFormat,
)

sealed class JobState {
    object Idle : JobState()
    data class Loading(val pct: Int) : JobState()
    data class Converting(val pct: Int) : JobState()
    data class Done(val outputUri: Uri, val outputName: String, val outputFormat: DocFormat) : JobState()
    data class Error(val message: String) : JobState()
}
