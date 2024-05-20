package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.idfedorov09.telegram.bot.data.model.CallbackData
import ru.idfedorov09.telegram.bot.data.model.SurveyAnswerOption

interface CallbackDataRepository : JpaRepository<CallbackData, Long> {

    fun findBySurveyAnswerOptionId(surveyAnswerOptionId: Long) : CallbackData
}
