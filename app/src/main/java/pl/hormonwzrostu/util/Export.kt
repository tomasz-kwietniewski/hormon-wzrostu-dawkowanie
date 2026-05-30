package pl.hormonwzrostu.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Zapisuje bajty do pamięci podręcznej i otwiera systemowy arkusz „Udostępnij". */
fun shareBytes(
    context: Context,
    bytes: ByteArray,
    fileName: String,
    mime: String,
    chooserTitle: String,
) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, fileName)
    file.outputStream().use { it.write(bytes) }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}
