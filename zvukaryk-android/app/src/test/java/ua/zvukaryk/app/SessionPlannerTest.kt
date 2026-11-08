package ua.zvukaryk.app

import org.junit.Assert.*
import org.junit.Test
import ua.zvukaryk.app.data.model.*
import ua.zvukaryk.app.domain.MasteryRules
import ua.zvukaryk.app.domain.SessionPlanner
import ua.zvukaryk.app.domain.SessionStep
import kotlin.random.Random

class SessionPlannerTest {
    private val planner = SessionPlanner(Random(42))

    private fun stateAt(level: Level, settings: Settings = Settings()) = AppState(
        onboarded = true, targetSounds = listOf("s"),
        progress = mapOf("s" to SoundProgress("s", level = level)), settings = settings,
    )

    @Test
    fun isolatedLevelHasNoPracticeButHasWarmupAndIsolatedGame() {
        val plan = planner.plan(stateAt(Level.ISOLATED), "s")
        assertTrue(plan.steps.any { it is SessionStep.Warmup })
        assertTrue(plan.steps.any { it is SessionStep.Placement })
        assertTrue(plan.steps.any { it is SessionStep.Isolated && it.trials == Level.ISOLATED.trialsPerSession })
        assertFalse(plan.steps.any { it is SessionStep.Practice })
    }

    @Test
    fun wordLevelPlanHitsEvidenceBasedTrialDose() {
        val plan = planner.plan(stateAt(Level.WORDS), "s")
        val practice = plan.steps.filterIsInstance<SessionStep.Practice>().single()
        assertEquals(Level.WORDS, practice.level)
        assertTrue("мета ≥ 30 продуктивних спроб", plan.productionTrials >= 30)
        assertTrue(plan.steps.any { it is SessionStep.Listening })
        // 12 слів на заняття, кожне повторюється
        assertTrue(practice.items.distinctBy { it.text }.size <= 12)
    }

    @Test
    fun syllablesAreBlockedNotShuffled() {
        val items = planner.practiceItems(ua.zvukaryk.app.data.content.Sounds.get("s"), Level.SYLLABLES, 0, 10)
        assertEquals(listOf("са", "со", "су", "си", "се"), items.take(5).map { it.text })
    }

    @Test
    fun warmupRotatesBetweenSessions() {
        val s = ua.zvukaryk.app.data.content.Sounds.get("r")
        val a = planner.pickWarmup(s, 0, 2).map { it.id }
        val b = planner.pickWarmup(s, 1, 2).map { it.id }
        assertNotEquals(a, b)
    }

    @Test
    fun settingsDisableWarmupAndEnableBreathing() {
        val plan = planner.plan(stateAt(Level.WORDS, Settings(warmupEnabled = false, breathingEnabled = true)), "s")
        assertFalse(plan.steps.any { it is SessionStep.Warmup })
        assertTrue(plan.steps.any { it is SessionStep.Breathing })
    }

    @Test
    fun storyStepAppearsOfflineFromSentencesLevel() {
        assertTrue(planner.plan(stateAt(Level.SENTENCES), "s").steps.any { it is SessionStep.Story })
        assertFalse(planner.plan(stateAt(Level.WORDS), "s").steps.any { it is SessionStep.Story })
        assertFalse(planner.plan(stateAt(Level.SENTENCES, Settings(claudeStoryEnabled = false)), "s").steps.any { it is SessionStep.Story })
    }

    @Test
    fun sessionBudgetScalesWithMinutes() {
        assertEquals(42, planner.trialBudget(7))
        assertEquals(16, planner.trialBudget(1))
        assertEquals(80, planner.trialBudget(30))
    }

    @Test
    fun differentiationFallsBackToSyllablesWhenNoPairs() {
        val sound = ua.zvukaryk.app.data.content.Sounds.get("shch")
        val pairs = planner.differentiationPairs(sound)
        assertTrue(pairs.isNotEmpty())
    }
}

class MasteryRulesTest {
    private fun rec(trials: Int, correct: Int, almost: Int = 0) = SessionRecord(1L, "s", Level.WORDS, trials, correct, almost, 300)

    @Test
    fun advancesOnlyAfterEnoughTrialsSessionsAndAccuracy() {
        var p = SoundProgress("s", level = Level.WORDS)
        p = MasteryRules.applySession(p, rec(trials = 25, correct = 24))
        assertEquals("одного заняття недостатньо", Level.WORDS, p.level)
        p = MasteryRules.applySession(p, rec(trials = 25, correct = 24))
        assertEquals(Level.PHRASES, p.level)
        assertEquals(0, p.trialsAtLevel)
    }

    @Test
    fun lowAccuracyDoesNotAdvance() {
        var p = SoundProgress("s", level = Level.WORDS)
        repeat(3) { p = MasteryRules.applySession(p, rec(trials = 30, correct = 15)) }
        assertEquals(Level.WORDS, p.level)
        assertFalse(MasteryRules.isStalled(p))
        repeat(3) { p = MasteryRules.applySession(p, rec(trials = 30, correct = 10)) }
        assertTrue("6 занять з точністю < 50 % → порада до логопеда", MasteryRules.isStalled(p))
    }

    @Test
    fun almostCountsAsHalfPoint() {
        var p = SoundProgress("s", level = Level.WORDS)
        p = MasteryRules.applySession(p, rec(trials = 20, correct = 10, almost = 10))
        assertEquals(0.75f, p.accuracyAtLevel, 0.001f)
    }

    @Test
    fun lastLevelMarksMastered() {
        var p = SoundProgress("s", level = Level.CONVERSATION)
        repeat(2) { p = MasteryRules.applySession(p, rec(trials = 20, correct = 20)) }
        assertTrue(p.mastered)
        assertEquals(Level.CONVERSATION, p.level)
    }
}
