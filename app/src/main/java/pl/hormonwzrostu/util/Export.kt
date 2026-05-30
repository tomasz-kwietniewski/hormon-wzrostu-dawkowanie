package pl.hormonwzrostu.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Zapisuje CSV do pamięci podręcznej i otwiera systemowy arkusz „Udostępnij". */
fun shareCsv(context: Context, csv: String, fileName: String, chooserTitle: String) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, fileName)
    // UTF-8 z BOM, aby Excel poprawnie rozpoznał polskie znaki.
    val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    file.outputStream().use { out ->
        out.write(bom)
        out.write(csv.toByteArray(Charsets.UTF_8))
    }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}
