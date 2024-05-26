package ru.idfedorov09.telegram.bot.repo

import ru.idfedorov09.telegram.bot.base.domain.repository.CallbackDataRepository
import ru.idfedorov09.telegram.bot.data.model.ECallbackData

interface ECallbackDataRepository : CallbackDataRepository<ECallbackData> {
    fun findBySurveyAnswerOptionId(surveyAnswerOptionId: Long) : ECallbackData
}
