package ru.idfedorov09.telegram.bot.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.Message
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.util.KeyboardUtil
import ru.idfedorov09.telegram.bot.util.MessageSenderUtil

@Service
class MessageSenderService(
    val bot: Executor,
    val userRepository: UserRepository,
) {

    companion object {
        private val log = LoggerFactory.getLogger(MessageSenderService::class.java)
    }

    /**
     * Отправляет пользователю сообщение
     * Выставляет клавиатуру, если это требуется
     */
    fun sendMessage(
        messageParams: MessageParams
    ): Message {
        return messageParams.run {
            if (replyMarkup == null) {
                trySendWithSwitchKeyboard(messageParams)
            } else {
                MessageSenderUtil.sendMessage(bot, messageParams)
            }
        }
    }

    private fun trySendWithSwitchKeyboard(
        messageParams: MessageParams
    ): Message {
        val chatId = messageParams.chatId.toLongOrNull()
        if (chatId == null || chatId < 0) {
            return MessageSenderUtil.sendMessage(bot, messageParams)
        }
        val user = userRepository.findByTui(messageParams.chatId)
            ?: throw Exception("User not found by tui=$chatId")
        if (user.isKeyboardSwitched) {
            return MessageSenderUtil.sendMessage(bot, messageParams)
        }
        val keyboard = KeyboardUtil.changeKeyboard(
            userKeyboardType = user.currentKeyboardType,
            user = user
        )
        val messageParamsWithKeyboard = messageParams.copy(replyMarkup = keyboard)

        return MessageSenderUtil.sendMessage(bot, messageParamsWithKeyboard).also {
            userRepository.updateKeyboardSwitchedForUserTui(messageParams.chatId, true)
        }
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