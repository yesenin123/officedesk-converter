package ru.officedesk.converter

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object Sharing {

    private const val READER_PKG = "ru.officedesk.reader"
    private const val READER_PKG_DEBUG = "ru.officedesk.reader.debug"

    fun openInReader(ctx: Context, uri: Uri, mime: String) {
        val readerInstalled = isReaderInstalled(ctx)
        if (readerInstalled != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                setPackage(readerInstalled)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { ctx.startActivity(intent); return } catch (_: ActivityNotFoundException) {}
        }
        // Если читалка не установлена — предложим установить
        suggestInstallReader(ctx)
    }

    fun openWith(ctx: Context, uri: Uri, mime: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            ctx.startActivity(Intent.createChooser(intent, "Открыть с помощью"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(ctx, "Нет приложения для открытия", Toast.LENGTH_SHORT).show()
        }
    }

    fun share(ctx: Context, uri: Uri, mime: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Поделиться"))
    }

    fun isReaderInstalled(ctx: Context): String? {
        return try {
            ctx.packageManager.getPackageInfo(READER_PKG, 0)
            READER_PKG
        } catch (_: Throwable) {
            try {
                ctx.packageManager.getPackageInfo(READER_PKG_DEBUG, 0)
                READER_PKG_DEBUG
            } catch (_: Throwable) { null }
        }
    }

    fun suggestInstallReader(ctx: Context) {
        // Открываем страницу читалки в RuStore
        val uri = Uri.parse("https://apps.rustore.ru/app/$READER_PKG")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            ctx.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(ctx, "Читалка не найдена. Установите OfficeDesk Reader.", Toast.LENGTH_LONG).show()
        }
    }
}
