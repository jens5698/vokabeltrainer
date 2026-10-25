package de.vokabeltrainer.app.data

/** Turns a Google Drive share link (or a bare file id) into a direct download URL. */
object DriveLinks {
    private val fileId = Regex("""/file/d/([A-Za-z0-9_-]{10,})""")
    private val sheetId = Regex("""docs\.google\.com/spreadsheets/d/([A-Za-z0-9_-]{10,})""")
    private val idParam = Regex("""[?&]id=([A-Za-z0-9_-]{10,})""")
    private val bareId = Regex("""^[A-Za-z0-9_-]{20,}$""")

    fun downloadUrl(link: String): String? {
        val text = link.trim()
        if (text.isEmpty()) return null
        sheetId.find(text)?.let {
            return "https://docs.google.com/spreadsheets/d/${it.groupValues[1]}/export?format=xlsx"
        }
        val id = fileId.find(text)?.groupValues?.get(1)
            ?: idParam.find(text)?.groupValues?.get(1)
            ?: bareId.find(text)?.value
        if (id != null) return "https://drive.google.com/uc?export=download&id=$id"
        return if (text.startsWith("https://") || text.startsWith("http://")) text else null
    }
}
