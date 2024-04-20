package ru.idfedorov09.telegram.bot.data.model.converter

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*
import ru.idfedorov09.telegram.bot.data.model.UserData

@Converter(autoApply = true)
class UserDataConverter(
    private val objectMapper: ObjectMapper
) : AttributeConverter<UserData, String> {

    override fun convertToDatabaseColumn(attribute: UserData?): String? {
        return attribute?.let { objectMapper.writeValueAsString(it) }
    }

    override fun convertToEntityAttribute(dbData: String?): UserData? {
        return dbData?.let { objectMapper.readValue(it, UserData::class.java) }
    }
}