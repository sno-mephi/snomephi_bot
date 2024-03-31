package ru.idfedorov09.telegram.bot.data.model.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.SentMessageStatus

@Converter(autoApply = true)
class SentMessageStatusTypeConverter : AttributeConverter<SentMessageStatus, String> {

    override fun convertToDatabaseColumn(attribute: SentMessageStatus?): String {
        return attribute?.name ?: SentMessageStatus.DEFAULT_STATUS.name
    }

    override fun convertToEntityAttribute(dbData: String?): SentMessageStatus? {
        return dbData?.let { SentMessageStatus.valueOf(it) }
    }
}