package ru.idfedorov09.telegram.bot.data.model

import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard

data class MessageParams (
    val chatId: String,
    val photo: InputFile? = null,
    val document: InputFile? = null,
    val text: String? = null,
    val replyMarkup: ReplyKeyboard? = InlineKeyboardMarkup().also { it.keyboard = emptyList() },
    val parseMode: String? = null,
    val fromChatId: String? = null,
    val messageId: Int? = null,
)