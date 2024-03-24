package ru.idfedorov09.telegram.bot.data.model

import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard

data class MessageParams(
    val chatId: String,
    val photo: InputFile? = null,
    val document: InputFile? = null,
    val sticker: InputFile? = null,
    val voice: InputFile? = null,
    val audio: InputFile? = null,
    val videoNote: InputFile? = null,
    val video: InputFile? = null,
    val text: String? = null,
    val replyMarkup: ReplyKeyboard? = null,
    val parseMode: String? = null,
    val fromChatId: String? = null,
    val messageId: Int? = null,
    val disableWebPagePreview: Boolean = false,
)
