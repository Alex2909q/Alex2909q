package ua.zvukaryk.app

import org.junit.Assert.*
import org.junit.Test
import ua.zvukaryk.app.data.content.ArticulationExercises
import ua.zvukaryk.app.data.content.Sounds

/** Перевірки цілісності логопедичного контенту: кожен звук має повний набір матеріалу. */
class ContentIntegrityTest {

    @Test
    fun everySoundHasMaterialForEveryLevel() {
        Sounds.all.forEach { s ->
            assertTrue("${s.id}: прямі склади", s.syllables.direct.size >= 4)
            assertTrue("${s.id}: обернені склади", s.syllables.reverse.size >= 4)
            assertTrue("${s.id}: слова на початку", s.words.initial.size >= 8)
            assertTrue("${s.id}: слова в середині", s.words.medial.size >= 6)
            assertTrue("${s.id}: слова в кінці", s.words.final.size >= 5)
            assertTrue("${s.id}: словосполучення", s.phrases.size >= 5)
            assertTrue("${s.id}: речення", s.sentences.size >= 5)
            assertTrue("${s.id}: чистомовки", s.chystomovky.size >= 6)
            assertTrue("${s.id}: скоромовки", s.tongueTwisters.size >= 3)
            assertTrue("${s.id}: вірші", s.poems.isNotEmpty())
            assertTrue("${s.id}: теми для розмови", s.conversationPrompts.size >= 2)
        }
    }

    @Test
    fun wordsContainTargetLetterInDeclaredPosition() {
        Sounds.all.forEach { s ->
            val letter = s.letter.lowercase()
            s.words.initial.forEach { assertTrue("${s.id} initial: ${it.text}", it.text.lowercase().startsWith(letter)) }
            s.words.medial.forEach { assertTrue("${s.id} medial: ${it.text}", it.text.lowercase().drop(1).dropLast(1).contains(letter)) }
            s.words.final.forEach {
                val t = it.text.lowercase().trimEnd('!')
                assertTrue("${s.id} final: ${it.text}", t.endsWith(letter) || (s.id == "c" && t.endsWith("ць")) || (s.id == "r" && t == "кенгуру"))
            }
        }
    }

    @Test
    fun everyWordHasEmoji() {
        Sounds.all.flatMap { it.words.all() }.forEach { assertTrue("Без картинки: ${it.text}", it.emoji.isNotBlank()) }
    }

    @Test
    fun minimalPairsReferenceExistingSoundsAndDiffer() {
        Sounds.all.forEach { s ->
            s.differentiation.forEach { p ->
                assertNotNull("${s.id}: невідомий парний звук ${p.otherSoundId}", Sounds.find(p.otherSoundId))
                assertNotEquals("${s.id}: пара з однакових слів", p.a.text, p.b.text)
                assertTrue("${s.id}: слово пари без цільової букви: ${p.a.text}", p.a.text.lowercase().contains(s.letter.lowercase()))
            }
        }
    }

    @Test
    fun warmupExerciseIdsResolve() {
        Sounds.all.forEach { s ->
            s.warmupExerciseIds.forEach { id -> assertNotNull("${s.id}: вправа $id", ArticulationExercises.all.firstOrNull { it.id == id }) }
            assertTrue(s.warmupExerciseIds.size >= 3)
        }
    }

    @Test
    fun ageNormsAreOrderedByCorrectionSequence() {
        val norms = Sounds.all.map { it.ageNormFrom }
        assertEquals(norms.sorted(), norms)
        assertEquals(listOf("s", "z", "c"), Sounds.recommendedForAge(3).map { it.id })
        assertTrue(Sounds.recommendedForAge(6).size == Sounds.all.size)
    }
}
