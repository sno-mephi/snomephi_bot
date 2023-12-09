package ru.idfedorov09.telegram.bot.fetchers.bot.userfetchers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.UserMessages
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

@Component
class RegistrationActionHandlerFetcher(
    private val updatesUtil: UpdatesUtil
) : GeneralFetcher() {

    companion object {
        private val log = LoggerFactory.getLogger(RegistrationFetcher::class.java)
    }

    @InjectData
    fun doFetch(
        update: Update,
        bot: Executor,
        userInfo: UserActualizedInfo
    ): UserActualizedInfo {
        if (!update.hasCallbackQuery()) return userInfo
        val chatId: String = updatesUtil.getChatId(update) ?: return userInfo
        val callbackData = update.callbackQuery.data

        val callbackMessage = update.callbackQuery.message
        val parameters = CallbackCommands.params(callbackData).firstOrNull()

        return when {
            CallbackCommands.USER_CONFIRM.isMatch(callbackData) -> onUserConfirm(
                callbackMessage,
                chatId,
                bot,
                parameters,
                userInfo
            )

            CallbackCommands.USER_DECLINE.isMatch(callbackData) -> onUserDecline(
                callbackMessage,
                chatId,
                bot,
                userInfo,
                parameters
            )

            CallbackCommands.USER_WITHOUT_GROUP.isMatch(callbackData) -> onUserWithoutGroup(
                callbackMessage,
                chatId,
                bot,
                userInfo,
            )

            else -> userInfo
        }

    }

    private fun onUserDecline(
        message: Message,
        chat: String,
        bot: Executor,
        userInfo: UserActualizedInfo,
        parameter: String?
    ): UserActualizedInfo {
        var user = userInfo

        parameter?.let {
            when (it) {
                "fullName" -> {
                    bot.execute(
                        SendMessage(
                            chat,
                            UserMessages.FullNameRequest(" заново")
                        )
                    )
                    user = user.copy(
                        lastUserActionType = LastUserActionType.REGISTRATION_ENTER_FULL_NAME
                    )
                }

                "studyGroup" -> {
                    bot.execute(
                        SendMessage().apply {
                            chatId = chat
                            text = UserMessages.GroupRequest(" заново")
                            replyMarkup = userWithoutGroupActionCallback()
                        }
                    )
                    user = user.copy(
                        lastUserActionType = LastUserActionType.REGISTRATION_ENTER_GROUP
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
        return user
    }

    private fun onUserConfirm(
        message: Message,
        chat: String,
        bot: Executor,
        parameter: String?,
        userInfo: UserActualizedInfo
    ): UserActualizedInfo {

        var user = userInfo

        parameter?.let {
            when (it) {
                "fullName" -> {
                    bot.execute(
                        SendMessage().apply {
                            chatId = chat
                            text = UserMessages.GroupRequest()
                            replyMarkup = userWithoutGroupActionCallback()
                        }
                    )
                    user = user.copy(
                        lastUserActionType = LastUserActionType.REGISTRATION_ENTER_GROUP
                    )
                }

                "studyGroup" -> {
                    bot.execute(
                        SendMessage(
                            chat,
                            UserMessages.RegistrationComplete(),
                        ),
                    )
                    user = user.copy(
                        lastUserActionType = LastUserActionType.DEFAULT
                    )
                }

                else -> {}
            }

            bot.execute(
                DeleteMessage(
                    chat,
                    message.messageId
                )
            )
        }
        return user
    }


    private fun onUserWithoutGroup(
        message: Message,
        chat: String,
        bot: Executor,
        userInfo: UserActualizedInfo
    ): UserActualizedInfo {
        bot.execute(
            SendMessage().apply {
                this.chatId = chat
                this.text = UserMessages.WithoutGroupConfirmation()
                this.replyMarkup = createActionsKeyboard("studyGroup")
            },
        )
        bot.execute(
            EditMessageText().apply {
                replyMarkup = null
                chatId = chat
                messageId = message.messageId
                text = message.text
            }
        )
        return userInfo.copy(lastUserActionType = LastUserActionType.REGISTRATION_CONFIRM_GROUP)
    }

}