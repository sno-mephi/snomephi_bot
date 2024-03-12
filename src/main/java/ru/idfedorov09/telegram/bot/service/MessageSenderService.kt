package ru.idfedorov09.telegram.bot.service

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.Message
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.util.MessageSenderUtil

@Service
class MessageSenderService(
    val bot: Executor
) {
    // TODO: логика добавления reply кнопок!
    fun sendMessage(messageParams: MessageParams): Message {
        return MessageSenderUtil.sendMessage(bot, messageParams)
    }

    fun editMessage(messageParams: MessageParams): Message {
        return MessageSenderUtil.editMessageText(bot, messageParams)
    }

    fun editMessageReplyMarkup(messageParams: MessageParams) {
        MessageSenderUtil.editMessageReplyMarkup(bot, messageParams)
    }

    fun deleteMessage(messageParams: MessageParams) {
        MessageSenderUtil.deleteMessage(bot, messageParams)
    }
}