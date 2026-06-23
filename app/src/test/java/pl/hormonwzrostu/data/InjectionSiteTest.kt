package pl.hormonwzrostu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InjectionSiteTest {

    @Test
    fun rotation_has8DistinctSites_allCombos() {
        assertEquals(8, InjectionSites.ROTATION.size)
        assertEquals(8, InjectionSites.ROTATION.toSet().size)
        val combos = InjectionSites.SIDES.flatMap { s -> InjectionSites.REGIONS.map { r -> "$s-$r" } }.toSet()
        assertEquals(combos, InjectionSites.ROTATION.toSet())
    }

    @Test
    fun rotation_neighborsDifferInSideAndRegion() {
        val r = InjectionSites.ROTATION
        for (i in r.indices) {
            val a = r[i].split("-")
            val b = r[(i + 1) % r.size].split("-")
            assertNotEquals("strona ma się zmieniać", a[0], b[0])
            assertNotEquals("region ma się zmieniać", a[1], b[1])
        }
    }

    @Test
    fun nextAfter_wrapsAndHandlesUnknown() {
        assertEquals(InjectionSites.ROTATION[1], InjectionSites.nextAfter(InjectionSites.ROTATION[0]))
        assertEquals(InjectionSites.ROTATION[0], InjectionSites.nextAfter(InjectionSites.ROTATION.last()))
        assertEquals(InjectionSites.ROTATION.first(), InjectionSites.nextAfter(null))
        assertEquals(InjectionSites.ROTATION.first(), InjectionSites.nextAfter("nieistnieje"))
    }

    @Test
    fun suggestedFor_usesLastBeforeDate() {
        // Brak historii -> pierwsze miejsce.
        assertEquals(InjectionSites.ROTATION.first(), InjectionSites.suggestedFor(emptyMap(), LocalDate.parse("2026-06-10")))
        // Ostatnie wcześniej = L-udo (indeks 0) -> podpowiedź to indeks 1.
        val sites = mapOf("2026-06-09" to "L-udo", "2026-06-08" to "P-posladek")
        assertEquals(InjectionSites.ROTATION[1], InjectionSites.suggestedFor(sites, LocalDate.parse("2026-06-10")))
    }

    @Test
    fun isValid_rejectsUnknownAndNull() {
        assertTrue(InjectionSites.isValid("L-udo"))
        assertFalse(InjectionSites.isValid("X-noga"))
        assertFalse(InjectionSites.isValid(null))
    }
}
