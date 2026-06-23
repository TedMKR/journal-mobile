package com.journal.core.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

fun shareBytesFile(
    context: Context,
    bytes: ByteArray,
    fileName: String,
    mimeType: String,
    chooserTitle: String
) {
    val safeName = fileName.ifBlank { "export.bin" }
        .replace(Regex("""[\\/:*?"<>|]"""), "_")
        .take(120)
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(exportDir, safeName)
    file.writeBytes(bytes)

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

fun shareTextFile(
    context: Context,
    text: String,
    fileName: String,
    mimeType: String = "text/csv",
    chooserTitle: String = "Export"
) {
    shareBytesFile(
        context = context,
        bytes = text.toByteArray(Charsets.UTF_8),
        fileName = fileName,
        mimeType = mimeType,
        chooserTitle = chooserTitle
    )
}
