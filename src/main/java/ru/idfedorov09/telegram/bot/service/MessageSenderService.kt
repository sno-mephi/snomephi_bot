package ru.idfedorov09.telegram.bot.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.Message
import ru.idfedorov09.telegram.bot.data.enums.SentMessageStatus
import ru.idfedorov09.telegram.bot.data.model.MessageByself
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.MessageByselfRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.util.KeyboardUtil
import ru.idfedorov09.telegram.bot.util.MessageSenderUtil
import ru.idfedorov09.telegram.bot.util.MessageSenderUtil.isTimeoutError

@Service
open class MessageSenderService(
    private val bot: Executor,
    private val userRepository: UserRepository,
    private val messageByselfRepository: MessageByselfRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(MessageSenderService::class.java)
    }

    @Scheduled(fixedDelay = 150)
    fun trySendMessagesWithTimeout() {
        runBlocking {
            val timeoutMessagesList =
                messageByselfRepository.findAllMessagesByStatus(status = SentMessageStatus.TIMEOUT)
            timeoutMessagesList.map {
                async {
                    processTryingResentMessage(it)
                }
            }.awaitAll()
        }
    }

    // TODO: поддержать не только отправку сообщений, но также и редактирование, удаление и прочее
    private fun processTryingResentMessage(
        messageByself: MessageByself
    ) {
        messageByself.messageParams ?: return
        runCatching {
            sendMessage(messageByself.messageParams, messageByself)
        }
    }

    /**
     * Отправляет пользователю сообщение
     * Выставляет клавиатуру, если это требуется
     * Если messageByself == null, то сохраняет в БД запись о сообщении со статусом отправки
     */
    fun sendMessage(
        messageParams: MessageParams,
        messageByself: MessageByself? = null
    ): Message {
        val sentMessageRow = messageByself ?:  messageByselfRepository.save(
            MessageByself(
                status = SentMessageStatus.DEFAULT_STATUS
            )
        )
        return messageParams.run {
            runCatching {
                sendMessageWithResolveType(messageParams)
            }.onFailure { e ->
                val messageStatus = when {
                    e.isTimeoutError() -> SentMessageStatus.TIMEOUT
                    else -> SentMessageStatus.UNKNOWN_ERROR
                }
                messageByselfRepository.save(sentMessageRow.copy(status = messageStatus))
            }.onSuccess {
                messageByselfRepository.save(
                    sentMessageRow.copy(
                        status = SentMessageStatus.OK
                    )
                )
            }.getOrThrow()
        }
    }

    /**
     * Отправляет пользователю сообщение
     * Выставляет клавиатуру, если это требуется
     * Инкапсулировано для обработки ошибок
     */
    private fun sendMessageWithResolveType(messageParams: MessageParams): Message {
        return messageParams.run {
            if (replyMarkup == null) {
                trySendWithSwitchKeyboard(messageParams)
            } else {
                MessageSenderUtil.sendMessage(bot, messageParams)
            }
        }
    }

    private fun trySendWithSwitchKeyboard(messageParams: MessageParams): Message {
        val chatId = messageParams.chatId.toLongOrNull()
        if (chatId == null || chatId < 0) {
            return MessageSenderUtil.sendMessage(bot, messageParams)
        }
        val user =
            userRepository.findByTui(messageParams.chatId)
                ?: throw Exception("User not found by tui=$chatId")
        if (user.isKeyboardSwitched) {
            return MessageSenderUtil.sendMessage(bot, messageParams)
        }
        val keyboard =
            KeyboardUtil.changeKeyboard(
                userKeyboardType = user.currentKeyboardType,
                user = user,
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
