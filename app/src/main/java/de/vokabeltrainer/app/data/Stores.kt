package de.vokabeltrainer.app.data

import android.content.Context

/** Learning progress per vocab id, kept in SharedPreferences as "known,unknown". */
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    fun loadAll(): Map<String, Progress> = prefs.all.mapNotNull { (key, value) ->
        val parts = (value as? String)?.split(',') ?: return@mapNotNull null
        val known = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val unknown = parts.getOrNull(1)?.toIntOrNull() ?: 0
        key to Progress(known, unknown)
    }.toMap()

    fun save(id: String, progress: Progress) {
        prefs.edit().putString(id, "${progress.known},${progress.unknown}").apply()
    }

    fun reset(language: Language) {
        val prefix = "${language.fileSuffix}|"
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach { editor.remove(it) }
        editor.apply()
    }
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun link(language: Language): String = prefs.getString("link_${language.fileSuffix}", "").orEmpty()

    fun setLink(language: Language, link: String) {
        prefs.edit().putString("link_${language.fileSuffix}", link.trim()).apply()
    }

    /** Classic Leitner behaviour: "nicht gewusst" sends the word back to Stapel 1. */
    var resetOnWrong: Boolean
        get() = prefs.getBoolean("reset_on_wrong", false)
        set(value) = prefs.edit().putBoolean("reset_on_wrong", value).apply()
}
