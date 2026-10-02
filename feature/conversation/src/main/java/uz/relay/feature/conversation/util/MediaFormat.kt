package uz.relay.feature.conversation.util

import android.content.res.Resources
import android.webkit.MimeTypeMap
import uz.relay.feature.conversation.R
import java.util.Locale

/*
 * Media bubble'lar va ko'ruvchi uchun o'lcham, progress va davomiylik formatlari. Raqamlar Locale.US bilan
 * formatlanadi (nuqta ajratgich barqaror bo'lsin), birlik ("KB"/"MB") esa tarjima resurslaridan.
 */

/** "2.4 MB" / "820 KB" (spec: "1.4 / 3.2 MB"). 1 MB = 1024 KB — Android fayl menejerlari kabi. */
fun formatSize(bytes: Long, resources: Resources): String {
    val kb = bytes / 1024.0
    return if (kb < 1024) resources.getString(R.string.size_kb, String.format(Locale.US, "%.0f", kb.coerceAtLeast(1.0)))
    else resources.getString(R.string.size_mb, String.format(Locale.US, "%.1f", kb / 1024))
}

/** "1.4 / 3.2 MB": birlik bir marta, oxirida. */
fun formatSizeProgress(done: Long, total: Long, resources: Resources): String {
    val totalMb = total / 1024.0 / 1024.0
    return if (totalMb >= 1) {
        resources.getString(
            R.string.size_mb,
            resources.getString(R.string.size_progress, String.format(Locale.US, "%.1f", done / 1024.0 / 1024.0), String.format(Locale.US, "%.1f", totalMb))
        )
    } else {
        resources.getString(
            R.string.size_kb,
            resources.getString(R.string.size_progress, String.format(Locale.US, "%.0f", done / 1024.0), String.format(Locale.US, "%.0f", total / 1024.0))
        )
    }
}

/** Video davomiyligi: "0:42", "12:05", "1:02:03". */
fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.US, "%d:%02d", minutes, seconds)
}

/** Fayl kartasidagi tur yorlig'i: nom kengaytmasi, bo'lmasa MIME'dan ("PDF", "XLSX"). */
fun fileTypeLabel(fileName: String?, mimeType: String): String {
    val fromName = fileName?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() && it.length <= 5 }
    val fromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    return (fromName ?: fromMime ?: "FILE").uppercase(Locale.ROOT)
}
