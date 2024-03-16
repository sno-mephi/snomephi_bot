package ru.idfedorov09.telegram.bot.util

import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.executor.Executor

object MessageSenderUtil {
    fun sendMessage(
        bot: Executor,
        messageParams: MessageParams,
    ): Message {
        messageParams.apply {
            return when {
                fromChatId != null -> forwardMessage(bot, messageParams)
                else -> commonMessage(bot, messageParams)
            }
        }
    }

    fun editMessageText(
        bot: Executor,
        params: MessageParams,
    ): Message {
        return params.run {
            if (messageId == null || text == null) {
                throw NullPointerException("messageId and text in editing message should be not null.")
            }
            if (replyMarkup != null && replyMarkup !is InlineKeyboardMarkup) {
                throw IllegalArgumentException("Incorrect type of keyboard!")
            }

            bot.execute(
                EditMessageText().also {
                    it.chatId = chatId
                    it.messageId = messageId
                    it.text = text
                    it.replyMarkup = replyMarkup as? InlineKeyboardMarkup
                    it.parseMode = parseMode
                },
            ) as Message
        }
    }

    fun deleteMessage(
        bot: Executor,
        params: MessageParams,
    ) {
        params.apply {
            messageId ?: throw NullPointerException("messageId is should be not null!")

            bot.execute(
                DeleteMessage().also {
                    it.chatId = chatId
                    it.messageId = messageId
                },
            )
        }
    }

    fun editMessageReplyMarkup(
        bot: Executor,
        params: MessageParams,
    ) {
        params.apply {
            if (messageId == null) {
                throw NullPointerException("messageId in editing message should be not null.")
            }

            if (replyMarkup != null && replyMarkup !is InlineKeyboardMarkup) {
                throw IllegalArgumentException("Incorrect type of keyboard!")
            }

            bot.execute(
                EditMessageReplyMarkup().also {
                    it.chatId = chatId
                    it.messageId = messageId
                    it.replyMarkup = replyMarkup as? InlineKeyboardMarkup
                },
            )
        }
    }

    private fun commonMessage(
        bot: Executor,
        params: MessageParams,
    ): Message {
        return params.run {
            if (text == null && photo == null && document == null) {
                throw NullPointerException("Text or photo or document should be not null.")
            }
            if (document != null) {
                bot.execute(
                    SendDocument().also {
                        it.document = document
                        it.caption = text
                        it.chatId = chatId
                        it.replyMarkup = replyMarkup
                        it.parseMode = parseMode
                    },
                )
            } else if (photo != null) {
                bot.execute(
                    SendPhoto().also {
                        it.chatId = chatId
                        it.caption = text
                        it.photo = photo
                        it.replyMarkup = replyMarkup
                        it.parseMode = parseMode
                    },
                )
            } else {
                bot.execute(
                    SendMessage().also {
                        it.chatId = chatId
                        it.text = text!!
                        it.replyMarkup = replyMarkup
                        it.parseMode = parseMode
                    },
                )
            }
        }
    }

    private fun forwardMessage(
        bot: Executor,
        params: MessageParams,
    ): Message {
        return params.run {
            if (fromChatId == null && messageId == null) {
                throw NullPointerException("fromChatId and messageId should be not null.")
            }
            bot.execute(
                ForwardMessage().also {
                    it.chatId = chatId
                    it.fromChatId = fromChatId!!
                    it.messageId = messageId!!
                },
            )
        }
    }
}
