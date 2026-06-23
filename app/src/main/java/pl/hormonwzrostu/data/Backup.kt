package pl.hormonwzrostu.data

import kotlinx.serialization.Serializable

/** Pełna kopia danych aplikacji (do ręcznego eksportu/importu). */
@Serializable
data class Backup(
    val version: Int = 4,
    val schedule: Schedule = Schedule(),
    val intake: Set<String> = emptySet(),
    val comments: Map<String, String> = emptyMap(),
    val lang: String = "",
    /** Data ISO -> faktycznie podana dawka (mg). Brak wpisu = dawka wg planu. */
    val doses: Map<String, Double> = emptyMap(),
    /** Dni jawnie oznaczone „Pominięto" (ISO). Puste w starszych kopiach. */
    val skipped: Set<String> = emptySet(),
    /** Dni otwarcia nowej ampułki — ręczne re-kotwice cyklu (ISO). Puste w starszych kopiach. */
    val ampouleStarts: Set<String> = emptySet(),
    /** Data ISO -> token miejsca wkłucia (np. „L-udo"). Puste w starszych kopiach. */
    val sites: Map<String, String> = emptyMap(),
)
