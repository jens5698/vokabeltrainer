package de.vokabeltrainer.app.data

import android.content.Context
import android.util.Xml
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class VocabRepository(private val context: Context) {

    private val reader = XlsxReader { Xml.newPullParser() }

    private fun cacheFile(language: Language) = File(context.filesDir, "Vokabeln_${language.fileSuffix}.xlsx")

    /** Last successfully downloaded copy, or null if there is none yet. */
    fun loadCached(language: Language): Pair<List<Vocab>, Long>? {
        val file = cacheFile(language)
        if (!file.exists()) return null
        return runCatching { parse(language, file.readBytes()) to file.lastModified() }.getOrNull()
    }

    /** Downloads the file, parses it and only then replaces the cached copy. */
    fun download(language: Language, link: String): List<Vocab> {
        val url = DriveLinks.downloadUrl(link) ?: throw IOException("Kein gültiger Link hinterlegt")
        val bytes = fetch(url)
        if (bytes.size < 4 || bytes[0] != 'P'.code.toByte() || bytes[1] != 'K'.code.toByte()) {
            throw IOException("Keine xlsx-Datei erhalten. Ist die Datei für „Jeder mit dem Link“ freigegeben?")
        }
        val vocab = parse(language, bytes)
        cacheFile(language).writeBytes(bytes)
        return vocab
    }

    private fun parse(language: Language, bytes: ByteArray): List<Vocab> =
        VocabParser.parse(language, reader.readRows(bytes, preferredSheet = "Vokabeln"))

    private fun fetch(url: String): ByteArray {
        var current = URL(url)
        repeat(5) {
            val conn = current.openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = false
            try {
                when (val code = conn.responseCode) {
                    in 200..299 -> return conn.inputStream.use { it.readBytes() }
                    in 300..399 -> {
                        val location = conn.getHeaderField("Location") ?: throw IOException("Weiterleitung ohne Ziel")
                        current = URL(current, location)
                    }
                    else -> throw IOException("Download fehlgeschlagen (HTTP $code)")
                }
            } finally {
                conn.disconnect()
            }
        }
        throw IOException("Zu viele Weiterleitungen")
    }
}
