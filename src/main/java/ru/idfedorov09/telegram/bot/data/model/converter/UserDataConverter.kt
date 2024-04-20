package ru.idfedorov09.telegram.bot.data.model.converter

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*
import ru.idfedorov09.telegram.bot.data.model.UserData

@Converter(autoApply = true)
class UserDataConverter : AttributeConverter<UserData, String> {
    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: UserData?): String? {
        return attribute?.let { objectMapper.writeValueAsString(it) }
    }

    override fun convertToEntityAttribute(dbData: String?): UserData? {
        return dbData?.let { objectMapper.readValue(it, UserData::class.java) }
    }
}
