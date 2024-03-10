package ru.idfedorov09.telegram.bot.data.model.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType

@Converter(autoApply = true)
class LastUserActionTypeConverter : AttributeConverter<LastUserActionType, String> {

    override fun convertToDatabaseColumn(attribute: LastUserActionType?): String {
        return attribute?.name ?: LastUserActionType.DEFAULT.name
    }

    override fun convertToEntityAttribute(dbData: String?): LastUserActionType? {
        return dbData?.let { LastUserActionType.valueOf(it) }
    }
}