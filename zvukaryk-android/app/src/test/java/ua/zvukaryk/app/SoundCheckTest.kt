package ua.zvukaryk.app

import org.junit.Assert.*
import org.junit.Test
import ua.zvukaryk.app.speech.TtsManager.Status
import ua.zvukaryk.app.ui.components.voiceProblem

class SoundCheckTest {
    @Test
    fun explainsEveryReasonForSilence() {
        assertNull(voiceProblem(Status.READY, 0.7f))
        assertTrue(voiceProblem(Status.NO_ENGINE, 0.7f)!!.contains("немає синтезатора"))
        assertTrue(voiceProblem(Status.NO_UKRAINIAN, 0.7f)!!.contains("українською"))
        assertTrue(voiceProblem(Status.MISSING_DATA, 0.7f)!!.contains("не завантажено"))
        assertTrue(voiceProblem(Status.READY, 0f)!!.contains("Гучність"))
    }
}
