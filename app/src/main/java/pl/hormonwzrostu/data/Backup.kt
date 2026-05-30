package pl.hormonwzrostu.data

import kotlinx.serialization.Serializable

/** Pełna kopia danych aplikacji (do ręcznego eksportu/importu). */
@Serializable
data class Backup(
    val version: Int = 1,
    val schedule: Schedule = Schedule(),
    val intake: Set<String> = emptySet(),
    val comments: Map<String, String> = emptyMap(),
    val lang: String = "",
)
