package ua.zvukaryk.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import ua.zvukaryk.app.ai.AgentTools
import ua.zvukaryk.app.ai.ClaudeAgent
import ua.zvukaryk.app.data.model.*

class AgentToolsTest {
    private val state = AppState(
        onboarded = true, profile = ChildProfile(name = "Олесь", ageYears = 5),
        targetSounds = listOf("r", "sh", "unknown_sound"),
        progress = mapOf("r" to SoundProgress("r", level = Level.WORDS, trialsAtLevel = 30, correctAtLevel = 27, sessionsAtLevel = 2)),
        streakDays = 4,
    )

    @Test
    fun childProgressExposesTargetsAndSkipsUnknownIds() {
        val json = AgentTools.childProgress(state)
        assertEquals("Олесь", json["child"]!!.jsonObject["name"]!!.jsonPrimitive.content)
        val targets = json["target_sounds"]!!.jsonArray
        assertEquals(2, targets.size)
        val r = targets.first { it.jsonObject["sound_id"]!!.jsonPrimitive.content == "r" }.jsonObject
        assertEquals("WORDS", r["level"]!!.jsonPrimitive.content)
        assertEquals("90", r["accuracy_at_level"]!!.jsonPrimitive.content)
        assertEquals("true", r["ready_to_advance"]!!.jsonPrimitive.content)
    }

    @Test
    fun soundMaterialReturnsWordsAndErrorForUnknown() {
        val sh = AgentTools.soundMaterial("sh")
        assertTrue(sh["words"]!!.jsonObject["initial"]!!.jsonArray.size >= 8)
        assertTrue(sh["chystomovky"]!!.jsonArray.isNotEmpty())
        val bad = AgentTools.soundMaterial("xx")
        assertTrue(bad.containsKey("error"))
    }

    @Test
    fun speakToolCallsSpeakerAndReportsBack() {
        var spoken: String? = null
        val out = AgentTools.execute(AgentTools.SPEAK_TO_CHILD, mapOf("text" to "Привіт, Олесю!"), state) { spoken = it }
        assertEquals("Привіт, Олесю!", spoken)
        assertTrue(out.contains("Привіт"))
        val empty = AgentTools.execute(AgentTools.SPEAK_TO_CHILD, mapOf("text" to "  "), state) { fail("не має озвучувати порожнє") }
        assertTrue(empty.contains("Порожній"))
    }

    @Test
    fun unknownToolIsReportedNotThrown() {
        val out = AgentTools.execute("nope", emptyMap(), state) {}
        assertTrue(out.contains("Невідомий"))
    }

    @Test
    fun toolSpecsAreValidJsonSchemaInputs() {
        AgentTools.specs.forEach { spec ->
            assertTrue(spec.name.matches(Regex("[a-z_]+")))
            spec.required.forEach { assertTrue("${spec.name}: required '$it' без властивості", spec.properties.containsKey(it)) }
            // Перевірка, що опис серіалізується (без екзотичних типів)
            Json.encodeToString(kotlinx.serialization.serializer<Map<String, Map<String, String>>>(), spec.properties.mapValues { e -> e.value.mapValues { it.value.toString() } })
        }
    }

    @Test
    fun storySplitsIntoSentences() {
        val text = "Жук жив у житі. Він дружив із жабою!\nРазом вони бігли по доріжці… І знайшли пиріжок."
        val parts = ClaudeAgent.splitSentences(text)
        assertEquals(4, parts.size)
        assertEquals("Жук жив у житі.", parts[0])
        assertEquals("І знайшли пиріжок.", parts[3])
    }
}
