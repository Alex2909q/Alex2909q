package ua.zvukaryk.app.data.content

import ua.zvukaryk.app.data.model.SpeechSound

/** Реєстр усіх звуків у рекомендованому порядку корекції: свистячі → шиплячі → [л] → [р]. */
object Sounds {
    val all: List<SpeechSound> = listOf(
        SoundsSibilants.S, SoundsSibilants.Z, SoundsSibilants.C,
        SoundsHushing.SH, SoundsHushing.ZH, SoundsHushing.CH, SoundsHushing.SHCH,
        SoundsSonorants.L, SoundsSonorants.R,
    )
    private val byId = all.associateBy { it.id }
    val ids: Set<String> = byId.keys
    fun get(id: String): SpeechSound = byId.getValue(id)
    fun find(id: String): SpeechSound? = byId[id]

    /** Звуки, які доречно тренувати у цьому віці (не вище вікової норми + 0). Батьки можуть додати будь-який вручну. */
    fun recommendedForAge(ageYears: Int): List<SpeechSound> = all.filter { it.ageNormFrom <= ageYears }

    /** Слова для швидкого скринінгу: по 3 слова на звук у різних позиціях. */
    fun screeningWords(sound: SpeechSound) = listOfNotNull(
        sound.words.initial.firstOrNull(), sound.words.medial.firstOrNull(), sound.words.final.firstOrNull(),
    )
}

/** Колекція наліпок-нагород: видаються за завершене заняття, а не за окрему спробу. */
object Stickers {
    val all = listOf(
        "🦊", "🐻", "🐼", "🐨", "🦁", "🐯", "🐸", "🐙", "🦄", "🐬", "🦋", "🐞", "🌈", "⭐", "🌟", "🚀",
        "🎈", "🍓", "🍉", "🍦", "🎨", "🎸", "🏆", "🧸", "🦖", "🐳", "🦜", "🌻", "🍀", "🎁", "🪁", "🛸",
    )
}
