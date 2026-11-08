package ua.zvukaryk.app

import org.junit.Assert.*
import org.junit.Test
import ua.zvukaryk.app.ai.ClaudeAgent
import ua.zvukaryk.app.data.content.HelperContent
import ua.zvukaryk.app.data.content.Sounds
import ua.zvukaryk.app.data.model.*
import ua.zvukaryk.app.domain.Advisor
import ua.zvukaryk.app.domain.SessionFeedback

/** Офлайн-помічник: контент для кожного звука, порадник і підсумки без інтернету. */
class OfflineHelperTest {

    @Test
    fun everySoundHasExplanationsRiddlesAndStories() {
        Sounds.all.forEach { s ->
            assertTrue("${s.id}: пояснення", (HelperContent.explanations[s.id]?.size ?: 0) >= 3)
            assertTrue("${s.id}: загадки", (HelperContent.riddles[s.id]?.size ?: 0) >= 3)
            assertTrue("${s.id}: казки", (HelperContent.stories[s.id]?.size ?: 0) >= 2)
        }
    }

    @Test
    fun riddleAnswersComeFromTheSoundWordList() {
        Sounds.all.forEach { s ->
            val words = s.words.all().map { it.text.lowercase() }.toSet()
            HelperContent.riddles.getValue(s.id).forEach { r ->
                assertTrue("${s.id}: відгадки «${r.answer}» немає серед слів звука", r.answer.lowercase() in words)
                assertFalse("${s.id}: загадка не має містити відгадку", r.text.lowercase().contains(r.answer.lowercase()))
            }
        }
    }

    @Test
    fun everyStorySentenceContainsTheTargetSoundAndIsShort() {
        Sounds.all.forEach { s ->
            val letter = s.letter.lowercase()
            HelperContent.stories.getValue(s.id).forEach { story ->
                assertTrue(story.size in 5..8)
                story.forEach { sentence ->
                    assertTrue("${s.id}: у реченні немає звука: $sentence", sentence.lowercase().contains(letter))
                    assertTrue("${s.id}: задовге речення для повторення: $sentence", sentence.split(" ").size <= 10)
                }
            }
        }
    }

    @Test
    fun advisorMatchesTypicalParentQuestions() {
        assertEquals("substitution", Advisor.match("Дитина каже либа замість риба")?.id)
        assertEquals("logoped", Advisor.match("Коли йти до логопеда?")?.id)
        assertEquals("plan", Advisor.match("Склади план на тиждень")?.id)
        assertEquals("distortion", Advisor.match("У нас горлове Р")?.id)
        assertNull(Advisor.match("яка погода завтра"))
        assertTrue(Advisor.answer("яка погода завтра", AppState()).contains("без інтернету"))
    }

    @Test
    fun personalAdviceReflectsProgressAndStall() {
        val stalled = SoundProgress("r", level = Level.WORDS, trialsAtLevel = 120, correctAtLevel = 30, sessionsAtLevel = 6)
        val state = AppState(profile = ChildProfile("Олесь", 5), targetSounds = listOf("r"), progress = mapOf("r" to stalled), totalSessions = 6)
        val advice = Advisor.personalAdvice(state)
        assertTrue(advice.contains("логопеду"))
        assertTrue(Advisor.personalAdvice(AppState()).contains("Ще не обрано"))
        assertTrue(Advisor.weekPlan(state).contains("Р"))
    }

    @Test
    fun sessionFeedbackPraisesAndGivesActionableTip() {
        val sh = Sounds.get("sh")
        val good = SessionFeedback.build("Марійка", sh, Level.WORDS, trials = 40, correct = 36, almost = 2)
        assertTrue(good.praise.contains("Марійка"))
        assertTrue(good.tip.contains("Словосполучення"))
        val hard = SessionFeedback.build("", sh, Level.WORDS, trials = 30, correct = 5, almost = 2)
        assertFalse(hard.praise.contains("погано"))
        assertTrue(hard.tip.contains("логопед"))
        val none = SessionFeedback.build("Ія", sh, Level.WORDS, trials = 0, correct = 0, almost = 0)
        assertTrue(none.tip.contains("той самий час"))
    }

    @Test
    fun splitterKeepsDialogueAttribution() {
        val parts = ClaudeAgent.splitSentences("Соня сказала: «Ходімо в сад!» У саду росли сливи. «Сідай!» — сказав лис. Смачно!")
        assertEquals(listOf("Соня сказала: «Ходімо в сад!»", "У саду росли сливи.", "«Сідай!» — сказав лис.", "Смачно!"), parts)
    }
}
