package de.vokabeltrainer.app.data

enum class Language(val label: String, val fileSuffix: String) {
    ENGLISH("Englisch", "E"),
    FRENCH("Französisch", "F"),
}

data class Vocab(
    val language: Language,
    val topic: String,
    val foreign: String,
    val german: String,
    val example: String,
    val exampleGerman: String,
) {
    /** Stable key for the learning progress; independent of row order and topic. */
    val id: String get() = "${language.fileSuffix}|$foreign|$german"
}

data class Progress(val known: Int = 0, val unknown: Int = 0) {
    /** Stapel 1..5: 0, 1, 2, 3 and 4+ times known. */
    val stack: Int get() = known.coerceIn(0, STACKS - 1) + 1

    companion object {
        const val STACKS = 5
    }
}

object VocabParser {
    const val NO_TOPIC = "Ohne Thema"

    /** Columns: Thema, Fremdsprache, Deutsch, Beispielsatz, Beispielsatz (Deutsch). */
    fun parse(language: Language, rows: List<List<String>>): List<Vocab> =
        rows.mapNotNull { row ->
            fun cell(i: Int) = row.getOrNull(i)?.trim().orEmpty()
            val foreign = cell(1)
            val german = cell(2)
            when {
                foreign.isEmpty() || german.isEmpty() -> null
                german.equals("Deutsch", ignoreCase = true) -> null // header row
                else -> Vocab(
                    language = language,
                    topic = cell(0).ifEmpty { NO_TOPIC },
                    foreign = foreign,
                    german = german,
                    example = cell(3),
                    exampleGerman = cell(4),
                )
            }
        }
}
