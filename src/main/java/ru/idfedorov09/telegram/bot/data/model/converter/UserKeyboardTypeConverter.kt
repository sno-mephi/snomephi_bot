package ru.idfedorov09.telegram.bot.data.model.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import ru.idfedorov09.telegram.bot.data.enums.UserKeyboardType

@Converter(autoApply = true)
class UserKeyboardTypeConverter : AttributeConverter<UserKeyboardType, String> {

    override fun convertToDatabaseColumn(attribute: UserKeyboardType?): String? {
        return attribute?.name
    }

    override fun convertToEntityAttribute(dbData: String?): UserKeyboardType {
        return dbData?.let { UserKeyboardType.valueOf(it) } ?: UserKeyboardType.WITHOUT_KEYBOARD
    }
}