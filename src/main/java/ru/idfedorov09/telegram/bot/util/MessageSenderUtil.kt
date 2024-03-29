package ru.idfedorov09.telegram.bot.util

import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.send.*
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
                    it.disableWebPagePreview = disableWebPagePreview
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
            if (text == null && photo == null && document == null && sticker == null && voice == null &&
                videoNote == null && audio == null && video == null
            ) {
                throw NullPointerException(
                    "Text or photo or document or sticker or voice or videoNote or " +
                        "audio or video should be not null.",
                )
            }
            when {
                sticker != null -> sendSticker(bot, this)
                voice != null -> sendVoice(bot, this)
                videoNote != null -> sendVideoNote(bot, this)
                video != null -> sendVideo(bot, this)
                audio != null -> sendAudio(bot, this)
                document != null -> sendDocument(bot, this)
                photo != null -> sendPhoto(bot, this)
                else -> sendText(bot, this)
            }
        }
    }

    private fun sendSticker(
        bot: Executor,
        params: MessageParams,
    ): Message {
        return params.run {
            bot.execute(
                SendSticker().also {
                    it.sticker = sticker!!
                    it.chatId = chatId
                },
            )
        }
    }

    private fun sendVoice(
        bot: Executor,
        params: MessageParams,
    ): Message {
        return params.run {
            bot.execute(
                SendVoice().also {
                    it.voice = voice!!
                    it.chatId = chatId
                },
            )
        }
    }

    private fun sendVideoNote(
        bot: Executor,
        params: MessageParams,
    ): Message {
        return params.run {
            bot.execute(
                SendVideoNote().also {
                    it.videoNote = videoNote!!
                    it.chatId = chatId
                },
            )
        }
    }

    private fun sendVideo(
        bot: Executor,
        params: MessageParams,
    ): Message {
        return params.run {
            bot.execute(
                SendVideo().also {
                    it.chatId = chatId
                    it.video = video!!
                    it.caption = text
                    it.replyMarkup = replyMarkup
                    it.parseMode = parseMode
                },
            )
        }
    }

    private fun sendAudio(
        bot: Executor,
        params: MessageParams,
    ): Message {
        return params.run {
            bot.execute(
                SendAudio().also {
                    it.chatId = chatId
                    it.audio = audio!!
                    it.caption = text
                    it.replyMarkup = replyMarkup
                    it.parseMode = parseMode
                },
            )
        }
    }

    private fun sendDocument(
        bot: Executor,
        params: MessageParams,
    ): Message {
        return params.run {
            bot.execute(
                SendDocument().also {
                    it.document = document!!
                    it.caption = text
                    it.chatId = chatId
                    it.replyMarkup = replyMarkup
                    it.parseMode = parseMode
                },
            )
        }
    }

    private fun sendPhoto(
        bot: Executor,
        params: MessageParams,
    ): Message {
        return params.run {
            bot.execute(
                SendPhoto().also {
                    it.chatId = chatId
                    it.caption = text
                    it.photo = photo!!
                    it.replyMarkup = replyMarkup
                    it.parseMode = parseMode
                },
            )
        }
    }

    private fun sendText(
        bot: Executor,
        params: MessageParams,
    ): Message {
        return params.run {
            bot.execute(
                SendMessage().also {
                    it.chatId = chatId
                    it.text = text!!
                    it.replyMarkup = replyMarkup
                    it.parseMode = parseMode
                    it.disableWebPagePreview = disableWebPagePreview
                },
            )
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

    fun userName(
        lastTgNick: String?,
        fullName: String?,
    ) = if (lastTgNick == null) {
        "$fullName"
    } else {
        "@$lastTgNick ($fullName)"
    }
}
