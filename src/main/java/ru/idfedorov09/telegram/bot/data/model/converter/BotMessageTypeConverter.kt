package ru.idfedorov09.telegram.bot.data.model.converter

import com.google.gson.Gson
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import ru.idfedorov09.telegram.bot.data.model.MessageParams

@Converter(autoApply = true)
class BotMessageTypeConverter(
    private val gson: Gson,
) : AttributeConverter<MessageParams, String> {

    override fun convertToDatabaseColumn(attribute: MessageParams?): String? {
        return attribute?.let { gson.toJson(it) }
    }

    override fun convertToEntityAttribute(dbData: String?): MessageParams? {
        return runCatching {
            gson.fromJson(dbData, MessageParams::class.java)
        }.getOrNull()
    }
}