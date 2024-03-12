package ru.idfedorov09.telegram.bot.service

import org.springframework.stereotype.Service
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.util.MessageSenderUtil

@Service
class MessageSenderService(
    val bot: Executor
) {
    // TODO: логика добавления reply кнопок!
    fun sendMessage(messageParams: MessageParams) {
        MessageSenderUtil.sendMessage(bot, messageParams)
    }
}