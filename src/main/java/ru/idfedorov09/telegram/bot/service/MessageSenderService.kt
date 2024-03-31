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

    @Scheduled(fixedDelay = 50)
    fun sendAllMessages() {
        runBlocking {
            val timeoutMessagesList = messageByselfRepository
                .findAllMessagesToSend()
                .groupBy { it.chatId }

            timeoutMessagesList.map {
                async {
                    processTryingResentMessage(it.value.firstOrNull())
                }
            }.awaitAll()
        }
    }

    // TODO: поддержать не только отправку сообщений, но также и редактирование, удаление и прочее
    private fun processTryingResentMessage(
        messageByself: MessageByself?
    ) {
        messageByself ?: return
        messageByself.messageParams ?: return
        sendMessage(
            messageParams = messageByself.messageParams,
            messageByself = messageByself,
            throwInError = false,
            quickSend = true
        )
    }

    /**
     * Отправляет пользователю сообщение
     * Выставляет клавиатуру, если это требуется
     * Если messageByself == null, то сохраняет в БД запись о сообщении со статусом отправки
     *
     * throwInError отвечате за выбрасывание исключения в случае возникновения ошибки.
     * Если при отправке возникла ошибка и throwInError == true, то выбрасываем исключение, если нет возвращаем null
     *
     * quickSend - моментальная отправка сообщения
     */
    fun sendMessage(
        messageParams: MessageParams,
        messageByself: MessageByself? = null,
        throwInError: Boolean = false,
        quickSend: Boolean = false,
    ): Message? {
        val sentMessageRow = messageByself ?:  messageByselfRepository.save(
            MessageByself(
                status = SentMessageStatus.DEFAULT_STATUS,
                chatId = messageParams.chatId,
                messageParams = messageParams
            )
        )
        if (!quickSend) return null
        val optionalResult = messageParams.run {
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
            }
        }
        return if (throwInError) optionalResult.getOrThrow() else optionalResult.getOrNull()
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
        messageParams.messageId ?: return
        MessageSenderUtil.deleteMessage(bot, messageParams)
    }
}
