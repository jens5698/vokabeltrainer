package de.vokabeltrainer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.vokabeltrainer.app.data.Language
import de.vokabeltrainer.app.data.Progress
import de.vokabeltrainer.app.data.ProgressStore
import de.vokabeltrainer.app.data.SettingsStore
import de.vokabeltrainer.app.data.Vocab
import de.vokabeltrainer.app.data.VocabRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

enum class Screen { HOME, QUIZ, SETTINGS, LIST }

enum class Direction(val label: (Language) -> String) {
    FOREIGN_TO_GERMAN({ "${it.label} → Deutsch" }),
    GERMAN_TO_FOREIGN({ "Deutsch → ${it.label}" }),
    MIXED({ "Gemischt" }),
}

data class LanguageData(
    val vocab: List<Vocab> = emptyList(),
    val loading: Boolean = false,
    val status: String = "",
    val isError: Boolean = false,
)

data class QuizCard(val vocab: Vocab, val showForeign: Boolean)

data class QuizState(
    val language: Language,
    val cards: List<QuizCard>,
    val index: Int = 0,
    val revealed: Boolean = false,
    val known: Int = 0,
    val unknown: Int = 0,
    val missed: List<Vocab> = emptyList(),
) {
    val finished: Boolean get() = index >= cards.size
    val current: QuizCard? get() = cards.getOrNull(index)
}

data class UiState(
    val screen: Screen = Screen.HOME,
    val language: Language = Language.ENGLISH,
    val data: Map<Language, LanguageData> = Language.entries.associateWith { LanguageData() },
    val progress: Map<String, Progress> = emptyMap(),
    /** Selected topics per language; null means "all topics". */
    val selectedTopics: Map<Language, Set<String>?> = emptyMap(),
    val selectedStacks: Set<Int> = (1..Progress.STACKS).toSet(),
    val direction: Direction = Direction.FOREIGN_TO_GERMAN,
    val quiz: QuizState? = null,
    /** Topic shown in the vocabulary list. */
    val listTopic: String? = null,
) {
    fun progressOf(vocab: Vocab): Progress = progress[vocab.id] ?: Progress()

    fun topicsOf(language: Language): List<String> =
        data[language]?.vocab.orEmpty().map { it.topic }.distinct().sortedWith(java.text.Collator.getInstance())

    fun isTopicSelected(language: Language, topic: String): Boolean =
        selectedTopics[language]?.contains(topic) ?: true

    /** Vocab of the current language in the selected topics (any stack). */
    fun vocabInSelectedTopics(): List<Vocab> =
        data[language]?.vocab.orEmpty().filter { isTopicSelected(language, it.topic) }

    fun quizCandidates(): List<Vocab> =
        vocabInSelectedTopics().filter { progressOf(it).stack in selectedStacks }
}

class TrainerViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = VocabRepository(app)
    private val progressStore = ProgressStore(app)
    val settings = SettingsStore(app)

    private val _state = MutableStateFlow(UiState(progress = progressStore.loadAll()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        Language.entries.forEach { loadLanguage(it) }
    }

    /** Shows the cached copy immediately, then refreshes it from Google Drive. */
    fun loadLanguage(language: Language) {
        viewModelScope.launch {
            val link = settings.link(language)
            val cached = withContext(Dispatchers.IO) { repository.loadCached(language) }
            setData(language) {
                LanguageData(
                    vocab = cached?.first ?: it.vocab,
                    loading = link.isNotBlank(),
                    status = if (link.isBlank()) "Kein Link hinterlegt – bitte in den Einstellungen eintragen." else "Lade aus Google Drive …",
                    isError = link.isBlank(),
                )
            }
            if (link.isBlank()) return@launch
            val result = withContext(Dispatchers.IO) { runCatching { repository.download(language, link) } }
            result.onSuccess { vocab ->
                setData(language) { LanguageData(vocab, status = "Geladen: ${vocab.size} Vokabeln, ${now()}") }
            }.onFailure { error ->
                val fallback = cached?.let { " Verwende Stand vom ${format(it.second)}." } ?: ""
                setData(language) {
                    it.copy(loading = false, status = (error.message ?: "Fehler beim Laden.") + fallback, isError = true)
                }
            }
        }
    }

    fun reloadAll() = Language.entries.forEach { loadLanguage(it) }

    private fun setData(language: Language, change: (LanguageData) -> LanguageData) =
        _state.update { s -> s.copy(data = s.data + (language to change(s.data.getValue(language)))) }

    fun selectLanguage(language: Language) = _state.update { it.copy(language = language) }

    fun toggleTopic(topic: String) = _state.update { s ->
        val all = s.topicsOf(s.language).toSet()
        val current = s.selectedTopics[s.language] ?: all
        val next = if (topic in current) current - topic else current + topic
        s.copy(selectedTopics = s.selectedTopics + (s.language to next))
    }

    fun selectAllTopics(selected: Boolean) = _state.update { s ->
        s.copy(selectedTopics = s.selectedTopics + (s.language to if (selected) null else emptySet()))
    }

    fun toggleStack(stack: Int) = _state.update { s ->
        val next = if (stack in s.selectedStacks) s.selectedStacks - stack else s.selectedStacks + stack
        s.copy(selectedStacks = next)
    }

    fun setDirection(direction: Direction) = _state.update { it.copy(direction = direction) }

    fun startQuiz(vocab: List<Vocab> = _state.value.quizCandidates()) = _state.update { s ->
        if (vocab.isEmpty()) return@update s
        val cards = vocab.shuffled().map { v ->
            val showForeign = when (s.direction) {
                Direction.FOREIGN_TO_GERMAN -> true
                Direction.GERMAN_TO_FOREIGN -> false
                Direction.MIXED -> Math.random() < 0.5
            }
            QuizCard(v, showForeign)
        }
        s.copy(screen = Screen.QUIZ, quiz = QuizState(s.language, cards))
    }

    fun openList(topic: String) = _state.update { it.copy(screen = Screen.LIST, listTopic = topic, quiz = null) }

    fun reveal() = _state.update { s -> s.copy(quiz = s.quiz?.copy(revealed = true)) }

    fun answer(knewIt: Boolean) = _state.update { s ->
        val quiz = s.quiz ?: return@update s
        val card = quiz.current ?: return@update s
        val old = s.progressOf(card.vocab)
        val new = when {
            knewIt -> old.copy(known = old.known + 1)
            settings.resetOnWrong -> old.copy(known = 0, unknown = old.unknown + 1)
            else -> old.copy(unknown = old.unknown + 1)
        }
        progressStore.save(card.vocab.id, new)
        s.copy(
            progress = s.progress + (card.vocab.id to new),
            quiz = quiz.copy(
                index = quiz.index + 1,
                revealed = false,
                known = quiz.known + if (knewIt) 1 else 0,
                unknown = quiz.unknown + if (knewIt) 0 else 1,
                missed = if (knewIt) quiz.missed else quiz.missed + card.vocab,
            ),
        )
    }

    fun open(screen: Screen) = _state.update { it.copy(screen = screen, quiz = if (screen == Screen.QUIZ) it.quiz else null) }

    fun saveSettings(links: Map<Language, String>, resetOnWrong: Boolean) {
        val changed = links.filter { (lang, link) -> link.trim() != settings.link(lang) }.keys
        links.forEach { (lang, link) -> settings.setLink(lang, link) }
        settings.resetOnWrong = resetOnWrong
        changed.forEach { loadLanguage(it) }
        open(Screen.HOME)
    }

    fun resetProgress(language: Language) {
        progressStore.reset(language)
        _state.update { it.copy(progress = progressStore.loadAll()) }
    }

    private fun now() = format(System.currentTimeMillis())
    private fun format(millis: Long) =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(millis))
}
