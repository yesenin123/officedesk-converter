package ru.officedesk.converter.engine

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import ru.officedesk.converter.DocFormat
import ru.officedesk.converter.JobInput
import ru.officedesk.converter.JobState
import java.io.File

/**
 * Запускает конвертацию задачи. Полностью локально, без сети.
 * Файлы пишутся в files/converted/ и доступны через FileProvider.
 */
object ConvertEngine {

    fun run(context: Context, job: JobInput): Flow<JobState> = flow {
        try {
            emit(JobState.Loading(5))

            // 1. Прочитать в IR
            val ir = Readers.read(context, job.sourceUri, job.from) { p ->
                // прогресс чтения 5..40
                // (Flow.emit недоступен из лямбды — кидаем периодические события через канал ниже)
            }
            emit(JobState.Loading(40))
            emit(JobState.Converting(45))

            // 2. Подготовить выходной файл
            val outDir = File(context.filesDir, "converted").apply { mkdirs() }
            val baseName = job.sourceName.substringBeforeLast('.', job.sourceName).take(80)
            val outName = "$baseName.${job.to.ext}"
            val outFile = File(outDir, outName).apply { if (exists()) delete() }

            // 3. Записать
            Writers.write(context, ir, outFile, job.to)
            emit(JobState.Converting(95))

            // 4. Раздать URI
            val outUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            emit(JobState.Done(outUri, outName, job.to))
        } catch (e: Throwable) {
            emit(JobState.Error(humanReadable(e)))
        }
    }.flowOn(Dispatchers.IO)

    private fun humanReadable(e: Throwable): String = when {
        e is OutOfMemoryError -> "Файл слишком большой для конвертации на устройстве."
        e.message?.contains("password", ignoreCase = true) == true -> "Документ защищён паролем."
        e.message?.contains("encrypted", ignoreCase = true) == true -> "Документ зашифрован."
        e is UnsupportedFormatException -> e.message ?: "Формат не поддерживается."
        else -> "Не удалось конвертировать: ${e.javaClass.simpleName}"
    }
}

class UnsupportedFormatException(msg: String) : RuntimeException(msg)
