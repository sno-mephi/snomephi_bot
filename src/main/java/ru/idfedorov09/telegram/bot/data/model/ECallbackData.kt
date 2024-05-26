package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import ru.idfedorov09.telegram.bot.base.domain.data.model.entity.CallbackData

/**
 * Таблица для обработки коллбэков
 */
@Entity(name = "callback_data_survey")
@DiscriminatorValue("survey")
class ECallbackData(
    id: Long? = null,
    chatId: String? = null,
    messageId: String? = null,
    callbackData: String? = null,
    metaText: String? = null,
    metaUrl: String? = null,

    @Column(name = "survey_answer_option_id")
    val surveyAnswerOptionId: Long? = null,
) : CallbackData(id, chatId, messageId, callbackData, metaText, metaUrl)
