package ua.zvukaryk.app.data.content

import ua.zvukaryk.app.data.model.ArticulationExercise
import ua.zvukaryk.app.data.model.ExerciseMode
import ua.zvukaryk.app.data.model.MouthPose

/**
 * Артикуляційна гімнастика «Казка про Веселий Язичок».
 * Виконується сидячи перед дзеркалом; статичні пози тримаємо на рахунок 5–10,
 * динамічні — 6–10 повторів. Дорослий спочатку показує сам.
 */
object ArticulationExercises {
    private val rest = MouthPose(open = 0.15f, smile = 0.2f, tongueY = 0.4f)

    val all: List<ArticulationExercise> = listOf(
        ArticulationExercise(
            id = "smile", name = "Парканчик", emoji = "😁",
            childCue = "Усміхнись широко, покажи зубки й тримай!",
            parentInstruction = "Губи розтягнуті в усмішці, зуби зімкнуті й видно. Тримати на рахунок до 10.",
            mode = ExerciseMode.HOLD, holdSeconds = 7,
            poseA = MouthPose(open = 0.12f, smile = 1f, tongueY = 0.6f, tongueWidth = 0.6f),
        ),
        ArticulationExercise(
            id = "tube", name = "Трубочка", emoji = "😗",
            childCue = "Витягни губки трубочкою, ніби хочеш подути на гарячий чай.",
            parentInstruction = "Губи витягнуті вперед і зімкнуті, зуби за губами. Тримати до 7.",
            mode = ExerciseMode.HOLD, holdSeconds = 6,
            poseA = MouthPose(open = 0.25f, round = 1f, smile = 0f, tongueY = 0.5f, teethVisible = false),
        ),
        ArticulationExercise(
            id = "smile_tube", name = "Усмішка — трубочка", emoji = "🔁",
            childCue = "Усміхнись… а тепер трубочка! І знову усмішка.",
            parentInstruction = "Чергуємо усмішку й трубочку, щелепа нерухома. 8–10 разів.",
            mode = ExerciseMode.REPEAT, repetitions = 8,
            poseA = MouthPose(open = 0.12f, smile = 1f, tongueY = 0.6f),
            poseB = MouthPose(open = 0.25f, round = 1f, smile = 0f, tongueY = 0.5f, teethVisible = false),
        ),
        ArticulationExercise(
            id = "spatula", name = "Лопаточка", emoji = "🥄",
            childCue = "Поклади широкий спокійний язичок на нижню губку, як млинець.",
            parentInstruction = "Рот трохи відкритий, широкий розслаблений язик лежить на нижній губі. Тримати до 10. Якщо язик тремтить або звужується — злегка пошльопати його губами («пя-пя-пя»).",
            mode = ExerciseMode.HOLD, holdSeconds = 8,
            poseA = MouthPose(open = 0.5f, smile = 0.5f, tongueY = 0.9f, tongueOut = 0.8f, tongueWidth = 1f),
        ),
        ArticulationExercise(
            id = "needle", name = "Голочка", emoji = "📍",
            childCue = "Зроби язичок тоненьким і гострим, як голочка, й витягни вперед.",
            parentInstruction = "Рот відкритий, вузький напружений язик витягнуто вперед. Кінчик не загинається. Тримати до 7.",
            mode = ExerciseMode.HOLD, holdSeconds = 6,
            poseA = MouthPose(open = 0.55f, smile = 0.4f, tongueY = 0.1f, tongueOut = 0.9f, tongueWidth = 0.1f),
        ),
        ArticulationExercise(
            id = "spatula_needle", name = "Лопаточка — голочка", emoji = "🔁",
            childCue = "Млинець… голочка… млинець… голочка!",
            parentInstruction = "Чергуємо широкий і вузький язик на нижній губі. 6–8 разів.",
            mode = ExerciseMode.REPEAT, repetitions = 6,
            poseA = MouthPose(open = 0.5f, smile = 0.5f, tongueY = 0.9f, tongueOut = 0.8f, tongueWidth = 1f),
            poseB = MouthPose(open = 0.55f, smile = 0.4f, tongueY = 0.1f, tongueOut = 0.9f, tongueWidth = 0.1f),
        ),
        ArticulationExercise(
            id = "hill", name = "Гірка (кицька сердиться)", emoji = "🐱",
            childCue = "Сховай кінчик язика за нижні зубки й вигни спинку гіркою.",
            parentInstruction = "Рот відкритий, кінчик язика впирається в нижні зуби, спинка язика піднята «гіркою». Тримати до 10. Ключова поза для свистячих.",
            mode = ExerciseMode.HOLD, holdSeconds = 8,
            poseA = MouthPose(open = 0.6f, smile = 0.6f, tongueY = 1f, tongueOut = 0f, tongueWidth = 0.9f, cup = -0.6f),
        ),
        ArticulationExercise(
            id = "brush_teeth", name = "Почистимо нижні зубки", emoji = "🪥",
            childCue = "Почисть язичком нижні зубки зсередини — туди-сюди.",
            parentInstruction = "Кінчик язика рухається вліво-вправо по внутрішньому боці нижніх зубів, щелепа нерухома. 8–10 разів.",
            mode = ExerciseMode.REPEAT, repetitions = 8,
            poseA = MouthPose(open = 0.5f, smile = 0.7f, tongueY = 1f, tongueOut = -0.4f, tongueWidth = 0.6f),
            poseB = MouthPose(open = 0.5f, smile = 0.7f, tongueY = 1f, tongueOut = 0.4f, tongueWidth = 0.6f),
        ),
        ArticulationExercise(
            id = "cup", name = "Чашечка", emoji = "☕",
            childCue = "Підніми краєчки язика вгору, ніби чашечка, щоб у неї можна було налити чай.",
            parentInstruction = "Рот широко відкритий, широкий язик піднятий, бічні краї й кінчик загнуті вгору. Краї не торкаються зубів. Тримати до 10. Ключова поза для шиплячих та [р].",
            mode = ExerciseMode.HOLD, holdSeconds = 8,
            poseA = MouthPose(open = 0.8f, smile = 0.3f, tongueY = -0.3f, tongueOut = 0.5f, tongueWidth = 1f, cup = 1f),
        ),
        ArticulationExercise(
            id = "jam", name = "Смачне варення", emoji = "🍯",
            childCue = "Оближи верхню губку широким язичком згори вниз — ням!",
            parentInstruction = "Широкий язик облизує верхню губу згори вниз (не з боку в бік). Нижня щелепа не допомагає. 6–8 разів.",
            mode = ExerciseMode.REPEAT, repetitions = 6,
            poseA = MouthPose(open = 0.5f, smile = 0.3f, tongueY = -0.9f, tongueOut = 0.9f, tongueWidth = 1f, cup = 0.5f),
            poseB = MouthPose(open = 0.4f, smile = 0.3f, tongueY = 0.3f, tongueOut = 0.3f, tongueWidth = 1f),
        ),
        ArticulationExercise(
            id = "painter", name = "Маляр", emoji = "🖌️",
            childCue = "Пофарбуй язичком стелю — від зубок углиб і назад.",
            parentInstruction = "Рот відкритий, кінчик язика погладжує піднебіння від верхніх зубів углиб і назад. Щелепа нерухома. 8–10 разів.",
            mode = ExerciseMode.REPEAT, repetitions = 8,
            poseA = MouthPose(open = 0.65f, smile = 0.3f, tongueY = -1f, tongueOut = 0.3f, tongueWidth = 0.7f),
            poseB = MouthPose(open = 0.65f, smile = 0.3f, tongueY = -1f, tongueOut = -0.5f, tongueWidth = 0.7f),
        ),
        ArticulationExercise(
            id = "mushroom", name = "Грибок", emoji = "🍄",
            childCue = "Приклей широкий язичок до стелі й тримай — це капелюшок грибка.",
            parentInstruction = "Рот відкритий, широкий язик присмоктано до піднебіння, вуздечка натягнута, як ніжка грибка. Тримати до 10.",
            mode = ExerciseMode.HOLD, holdSeconds = 7,
            poseA = MouthPose(open = 0.7f, smile = 0.5f, tongueY = -1f, tongueOut = 0f, tongueWidth = 1f),
        ),
        ArticulationExercise(
            id = "horse", name = "Конячка", emoji = "🐴",
            childCue = "Поцокай язичком, як конячка копитцями: цок-цок-цок!",
            parentInstruction = "Присмоктати язик до піднебіння і з клацанням відірвати. Щелепа нерухома, губи в усмішці. 10–12 разів.",
            mode = ExerciseMode.REPEAT, repetitions = 10,
            poseA = MouthPose(open = 0.6f, smile = 0.6f, tongueY = -1f, tongueWidth = 1f),
            poseB = MouthPose(open = 0.7f, smile = 0.6f, tongueY = 0.6f, tongueWidth = 0.9f),
        ),
        ArticulationExercise(
            id = "swing", name = "Гойдалка", emoji = "🎢",
            childCue = "Язичок гойдається: вгору за верхні зубки — вниз за нижні.",
            parentInstruction = "Рот відкритий, кінчик язика по черзі впирається у верхні та нижні зуби зсередини. Щелепа нерухома. 8–10 разів.",
            mode = ExerciseMode.REPEAT, repetitions = 8,
            poseA = MouthPose(open = 0.6f, smile = 0.4f, tongueY = -1f, tongueWidth = 0.7f),
            poseB = MouthPose(open = 0.6f, smile = 0.4f, tongueY = 1f, tongueWidth = 0.7f),
        ),
        ArticulationExercise(
            id = "clock", name = "Годинничок", emoji = "⏰",
            childCue = "Тік-так: язичок до одного куточка рота, потім до другого.",
            parentInstruction = "Рот трохи відкритий, вузький язик торкається по черзі лівого та правого куточка губ. 8–10 разів.",
            mode = ExerciseMode.REPEAT, repetitions = 8,
            poseA = MouthPose(open = 0.45f, smile = 0.5f, tongueY = 0.3f, tongueOut = 0.7f, tongueWidth = 0.3f, cup = 0f),
            poseB = MouthPose(open = 0.45f, smile = 0.5f, tongueY = 0.3f, tongueOut = -0.7f, tongueWidth = 0.3f, cup = 0f),
        ),
        ArticulationExercise(
            id = "turkey", name = "Індик", emoji = "🦃",
            childCue = "Поводи широким язичком по верхній губці вперед-назад і скажи «бл-бл-бл».",
            parentInstruction = "Широкий язик швидко рухається по верхній губі вперед-назад, спочатку беззвучно, потім із голосом «бл-бл-бл». 6–8 разів.",
            mode = ExerciseMode.REPEAT, repetitions = 8,
            poseA = MouthPose(open = 0.45f, smile = 0.2f, tongueY = -0.9f, tongueOut = 0.9f, tongueWidth = 1f),
            poseB = MouthPose(open = 0.45f, smile = 0.2f, tongueY = -0.7f, tongueOut = 0.2f, tongueWidth = 1f),
        ),
        ArticulationExercise(
            id = "drummer", name = "Дятлик", emoji = "🥁",
            childCue = "Постукай язичком за верхніми зубками: д-д-д-д!",
            parentInstruction = "Рот відкритий в усмішці, кінчик язика стукає по горбиках за верхніми зубами «д-д-д», поступово швидше. Щелепа нерухома. 10–15 ударів.",
            mode = ExerciseMode.REPEAT, repetitions = 12,
            poseA = MouthPose(open = 0.55f, smile = 0.7f, tongueY = -1f, tongueWidth = 0.6f),
            poseB = MouthPose(open = 0.55f, smile = 0.7f, tongueY = -0.6f, tongueWidth = 0.6f),
        ),
        ArticulationExercise(
            id = "steamboat", name = "Пароплав гуде", emoji = "🚢",
            childCue = "Прикуси кінчик язичка й гуди, як пароплав: и-и-и!",
            parentInstruction = "Кінчик язика злегка затиснутий між зубами, дитина довго тягне «и-и-и». Чути звук, близький до [л]. 3–4 рази по 3 секунди.",
            mode = ExerciseMode.HOLD, holdSeconds = 4,
            poseA = MouthPose(open = 0.3f, smile = 0.6f, tongueY = 0.2f, tongueOut = 0.5f, tongueWidth = 0.6f),
        ),
        ArticulationExercise(
            id = "bite_tongue", name = "Покусаємо язичок", emoji = "😬",
            childCue = "Легенько покусай язичок зубками від кінчика до серединки.",
            parentInstruction = "Усмішка, язик широкий, зуби легенько покусують його по всій поверхні. Розслаблює язик. 6–8 разів.",
            mode = ExerciseMode.REPEAT, repetitions = 6,
            poseA = MouthPose(open = 0.3f, smile = 0.8f, tongueY = 0.4f, tongueOut = 0.6f, tongueWidth = 1f),
            poseB = MouthPose(open = 0.15f, smile = 0.8f, tongueY = 0.4f, tongueOut = 0.6f, tongueWidth = 1f),
        ),
    )

    private val byId = all.associateBy { it.id }
    fun get(id: String): ArticulationExercise = byId.getValue(id)
    fun forIds(ids: List<String>): List<ArticulationExercise> = ids.mapNotNull { byId[it] }
}

/** Дихальні ігри з мікрофоном як сенсором видиху (подаються як гра, не як терапія). */
data class BreathingGame(val id: String, val title: String, val emoji: String, val instruction: String, val targetSeconds: Int)

object BreathingGames {
    val all = listOf(
        BreathingGame("balloon", "Надуй кульку", "🎈", "Вдихни носиком і повільно, довго дуй на телефон — кулька росте!", 4),
        BreathingGame("candle", "Задуй свічки", "🕯️", "Набери повітря і дмухни сильно й коротко — свічка згасне!", 1),
        BreathingGame("leaves", "Листочки на вітрі", "🍂", "Дуй плавно й рівно, щоб листочки довго летіли.", 5),
    )
}
