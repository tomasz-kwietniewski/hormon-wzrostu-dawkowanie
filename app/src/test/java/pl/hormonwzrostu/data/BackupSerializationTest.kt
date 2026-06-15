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
}
