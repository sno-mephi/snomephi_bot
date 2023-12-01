package ru.idfedorov09.telegram.bot.fetchers.bot.user_fetchers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.UserStrings
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.flow.ExpContainer
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

@Component
class RegistrationActionHandlerFetcher(
    private val updatesUtil: UpdatesUtil,
    private val userRepository: UserRepository
) : GeneralFetcher() {

    companion object {
        private val log = LoggerFactory.getLogger(RegistrationFetcher::class.java)
    }

    @InjectData
    fun doFetch(
        update: Update,
        bot: Executor
    ) {
        if (!update.hasCallbackQuery()) return
        val callbackData = update.callbackQuery.data

        val callbackMessage = update.callbackQuery.message
        val user: User = userRepository.findByTui(updatesUtil.getUserId(update) ?: "") ?: return
        val chatId: String = updatesUtil.getChatId(update) ?: return
        val parameters = CallbackCommands.params(callbackData).firstOrNull()

        when {
            CallbackCommands.USER_CONFIRM.isMatch(callbackData) -> onUserConfirm(
                callbackMessage,
                chatId,
                bot,
                parameters
            )
            CallbackCommands.USER_DECLINE.isMatch(callbackData) -> onUserDecline(
                callbackMessage,
                chatId,
                bot,
                user,
                parameters
            )
        }

    }

    private fun onUserDecline(
        message: Message,
        chat: String,
        bot: Executor,
        user: User,
        parameter: String?
    ) {
        parameter?.let {
            when (it) {
                "fullName" -> {
                    userRepository.save(user.copy(fullName = null))
                    bot.execute(
                        SendMessage().apply {
                            this.chatId = chatId
                            this.text = UserStrings.FullNameRequest(" заново")
                        }
                    )
                }

                "studyGroup" -> {
                    userRepository.save(user.copy(studyGroup = null))
                    bot.execute(
                        SendMessage().apply {
                            this.chatId = chatId
                            this.text = UserStrings.GroupRequest(" заново")
                        }
                    )
                }

                else -> {}
            }
            bot.execute(
                EditMessageText().apply {
                    replyMarkup = null
                    chatId = chat
                    messageId = message.messageId
                    text = message.text
                }
            )
        }
    }

    private fun onUserConfirm(
        message: Message,
        chatId: String,
        bot: Executor,
        parameter: String?
    ) {
        parameter?.let {
            when (it) {
                "fullName" -> {
                    bot.execute(
                        SendMessage().apply {
                            this.chatId = chatId
                            this.text = UserStrings.GroupRequest()
                        }
                    )
                }

                "studyGroup" -> {
                    bot.execute(
                        SendMessage(
                            chatId,
                            UserStrings.RegistrationComplete(),
                        ),
                    )
                }

                else -> {}
            }
            bot.execute(
                EditMessageText().apply {
                    this.replyMarkup = null
                    this.chatId = chatId
                    this.messageId = message.messageId
                    this.text = message.text
                }
            )
        }
    }
}