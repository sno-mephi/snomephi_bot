package ru.idfedorov09.telegram.bot.util

import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.Message
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.executor.Executor

object MessageSenderUtil {

    fun sendMessage(
        bot: Executor,
        messageParams: MessageParams
    ): Message {
        messageParams.apply {
            return when {
                fromChatId != null -> forwardMessage(bot, messageParams)
                else -> commonMessage(bot, messageParams)
            }
        }
    }

    private fun commonMessage(
        bot: Executor,
        params: MessageParams
    ): Message {
        return params.run {
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

    private fun forwardMessage(
        bot: Executor,
        params: MessageParams
    ): Message {
        return params.run {
            if (fromChatId == null && messageId == null) {
                throw throw IllegalArgumentException("fromChatId and messageId should be not null.")
            }
            bot.execute(
                ForwardMessage().also {
                    it.chatId = chatId
                    it.fromChatId = fromChatId!!
                    it.messageId = messageId!!
                }
            )
        }
    }
}