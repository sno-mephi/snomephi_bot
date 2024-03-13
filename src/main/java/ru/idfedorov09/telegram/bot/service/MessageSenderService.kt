package ru.idfedorov09.telegram.bot.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.Message
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.executor.TelegramPollingBot
import ru.idfedorov09.telegram.bot.fetchers.bot.ActualizeUserInfoFetcher
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.util.KeyboardUtil
import ru.idfedorov09.telegram.bot.util.MessageSenderUtil
import ru.mephi.sno.libs.flow.belly.FlowContext

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
        messageParams: MessageParams,
        context: FlowContext? = null,
    ): Message {
        return messageParams.run {
            if (replyMarkup == null) {
                trySendWithSwitchKeyboard(messageParams, context)
            } else {
                MessageSenderUtil.sendMessage(bot, messageParams)
            }
        }
    }

    private fun trySendWithSwitchKeyboard(
        messageParams: MessageParams,
        context: FlowContext? = null,
    ): Message {
        context ?: log.warn("Are you sure you're trying to change " +
                "the keyboard for a user who isn't in the flow context?")

        val userActualizedInfo = context?.get<UserActualizedInfo>()
        if (userActualizedInfo != null) {
            if (userActualizedInfo.isKeyboardSwitched || userActualizedInfo.tui != messageParams.chatId) {
                return MessageSenderUtil.sendMessage(bot, messageParams)
            }
            val keyboard = KeyboardUtil.changeKeyboard(
                userKeyboardType = userActualizedInfo.currentKeyboardType,
                user = userRepository.findById(userActualizedInfo.id!!).get()
            )
            val messageParamsWithKeyboard = messageParams.copy(replyMarkup = keyboard)
            return MessageSenderUtil.sendMessage(bot, messageParamsWithKeyboard).also {
                userActualizedInfo.isKeyboardSwitched = true
            }
        } else {
            val chatId = messageParams.chatId.toLongOrNull()
            if (chatId == null || chatId < 0) {
                return MessageSenderUtil.sendMessage(bot, messageParams)
            }
            val user = userRepository.findByTui(messageParams.chatId)
                ?: throw Exception("User not found by tui=$chatId")
            val keyboard = KeyboardUtil.changeKeyboard(
                userKeyboardType = user.currentKeyboardType,
                user = user
            )
            val messageParamsWithKeyboard = messageParams.copy(replyMarkup = keyboard)

            return MessageSenderUtil.sendMessage(bot, messageParamsWithKeyboard).also {
                userRepository.updateKeyboardSwitchedForUser(messageParams.chatId, true)
            }
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