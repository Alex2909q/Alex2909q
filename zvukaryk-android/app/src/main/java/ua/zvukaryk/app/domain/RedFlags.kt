package ua.zvukaryk.app.domain

import ua.zvukaryk.app.data.content.Sounds
import ua.zvukaryk.app.data.model.AppState

/** «Червоні прапорці»: коли варто показати дитину логопеду. Не діагноз, а підказка. */
object RedFlags {
    fun compute(state: AppState): List<String> {
        val f = mutableListOf<String>()
        val s = state.screening
        val age = state.profile.ageYears
        if (s.hearingConcern) f += "Є сумніви щодо слуху — спочатку перевірка слуху."
        if (s.vowelErrors) f += "Помилки у голосних — нетиповий тип помилок у будь-якому віці."
        if (s.inconsistent) f += "Непослідовна вимова того самого слова — потребує оцінки фахівця."
        if (s.stuttering) f += "Запинки / заїкання — окрема консультація логопеда."
        if (s.frustration) f += "Дитина засмучується через нерозуміння — не відкладайте консультацію."
        if (age >= 4 && s.intelligibilityParents <= 2) f += "У $age р. батьки розуміють дитину лише частково."
        if (age >= 5 && s.intelligibilityStrangers <= 1) f += "У $age р. незнайомі дорослі майже не розуміють дитину."
        s.problemSounds.mapNotNull { Sounds.find(it) }.filter { it.ageNormTo <= age }
            .forEach { f += "Звук ${it.letter} ще не сформовано у $age р. (норма до ${it.ageNormTo} р.)." }
        state.validTargets(Sounds.ids).map { state.progressFor(it) }.filter { MasteryRules.isStalled(it) }
            .forEach { f += "Звук ${Sounds.get(it.soundId).letter}: ${it.sessionsAtLevel} занять без прогресу." }
        return f
    }
}
