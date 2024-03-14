package ru.idfedorov09.telegram.bot.data.model.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus

@Converter(autoApply = true)
class QuestionStatusConverter : AttributeConverter<QuestionStatus, String> {
    override fun convertToDatabaseColumn(attribute: QuestionStatus?): String? {
        return attribute?.name
    }

    override fun convertToEntityAttribute(dbData: String?): QuestionStatus? {
        return dbData?.let { QuestionStatus.valueOf(it) }
    }
}
