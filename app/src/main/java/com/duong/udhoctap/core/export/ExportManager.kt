package com.duong.udhoctap.core.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ExportCard(val front: String, val back: String)

@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun exportToCsv(deckName: String, cards: List<ExportCard>): Intent {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeFileName = deckName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val file = File(exportsDir, "${safeFileName}_export.csv")

        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write("Mặt trước,Mặt sau\n")
            cards.forEach { card ->
                val front = card.front.replace("\"", "\"\"")
                val back = card.back.replace("\"", "\"\"")
                writer.write("\"$front\",\"$back\"\n")
            }
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Xuất bộ thẻ: $deckName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
