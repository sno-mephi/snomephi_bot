package ru.idfedorov09.telegram.bot.data.model.converter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.Gson
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import ru.idfedorov09.telegram.bot.data.model.MessageParams

@Converter(autoApply = true)
class BotMessageTypeConverter : AttributeConverter<MessageParams, String> {

    private val mapper = jacksonObjectMapper()
    override fun convertToDatabaseColumn(attribute: MessageParams?): String? {
        return mapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): MessageParams? {
        return dbData?.let { mapper.readValue(it) }
    }
}