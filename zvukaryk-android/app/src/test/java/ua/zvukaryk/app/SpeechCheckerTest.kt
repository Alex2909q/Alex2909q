package ua.zvukaryk.app

import org.junit.Assert.*
import org.junit.Test
import ua.zvukaryk.app.speech.SpeechChecker

class SpeechCheckerTest {
    @Test
    fun exactMatchIsOne() = assertEquals(1f, SpeechChecker.bestSimilarity("собака", listOf("Собака")), 0.001f)

    @Test
    fun substitutionLowersSimilarityButStaysClose() {
        val sim = SpeechChecker.bestSimilarity("шапка", listOf("сапка"))
        assertTrue(sim in 0.75f..0.9f)
    }

    @Test
    fun picksBestWordFromPhrase() {
        assertTrue(SpeechChecker.bestSimilarity("риба", listOf("велика риба пливе")) >= 0.99f)
    }

    @Test
    fun emptyHypothesesGiveZero() = assertEquals(0f, SpeechChecker.bestSimilarity("рак", emptyList()), 0.001f)
}
