package ru.idfedorov09.telegram.bot.util

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.executor.Executor

object MessageSenderUtil {

    fun sendMessage(bot: Executor, messageParams: MessageParams) {
        messageParams.apply {
            if (text == null && photo == null) {
                throw throw IllegalArgumentException("Text or photo should be not null.")
            }

            if (photo == null) {
                bot.execute(
                    SendMessage().also {
                        it.chatId = chatId
                        it.text = text!!
                        it.replyMarkup = replyMarkup
                        it.parseMode = parseMode
                    }
                )
            } else {
                bot.execute(
                    SendPhoto().also {
                        it.chatId = chatId
                        it.caption = text
                        it.photo = photo
                        it.replyMarkup = replyMarkup
                        it.parseMode = parseMode
                    }
                )
            }
        }
    }
}