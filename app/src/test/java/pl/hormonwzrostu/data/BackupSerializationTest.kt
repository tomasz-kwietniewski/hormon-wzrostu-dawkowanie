package pl.hormonwzrostu.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun roundTrip_withDoses() {
        val backup = Backup(version = 2, doses = mapOf("2026-01-12" to 0.8))
        val text = json.encodeToString(backup)
        val back = json.decodeFromString<Backup>(text)
        assertEquals(mapOf("2026-01-12" to 0.8), back.doses)
    }

    @Test
    fun oldJson_withoutDoses_decodesToEmpty() {
        val old = """{"version":1,"schedule":{},"intake":[],"comments":{},"lang":""}"""
        val back = json.decodeFromString<Backup>(old)
        assertTrue(back.doses.isEmpty())
    }

    @Test
    fun roundTrip_v3_withSkippedAndAmpouleStarts() {
        val backup = Backup(
            version = 3,
            skipped = setOf("2026-06-17", "2026-06-18"),
            ampouleStarts = setOf("2026-06-21"),
        )
        val back = json.decodeFromString<Backup>(json.encodeToString(backup))
        assertEquals(setOf("2026-06-17", "2026-06-18"), back.skipped)
        assertEquals(setOf("2026-06-21"), back.ampouleStarts)
    }

    @Test
    fun oldJson_v2_withoutNewFields_decodesToEmpty() {
        val old = """{"version":2,"schedule":{},"intake":[],"comments":{},"lang":"","doses":{}}"""
        val back = json.decodeFromString<Backup>(old)
        assertTrue(back.skipped.isEmpty())
        assertTrue(back.ampouleStarts.isEmpty())
    }
}
