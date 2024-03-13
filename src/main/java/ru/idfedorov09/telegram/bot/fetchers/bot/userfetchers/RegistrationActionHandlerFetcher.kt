package ru.idfedorov09.telegram.bot.fetchers.bot.userfetchers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.RegistrationMessageText
import ru.idfedorov09.telegram.bot.data.enums.UserKeyboardType
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.idfedorov09.telegram.bot.service.SwitchKeyboardService
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.FlowContext
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

@Component
class RegistrationActionHandlerFetcher(
    private val updatesUtil: UpdatesUtil,
    private val messageSenderService: MessageSenderService,
    private val switchKeyboardService: SwitchKeyboardService,
) : GeneralFetcher() {

    companion object {
        private val log = LoggerFactory.getLogger(RegistrationFetcher::class.java)
    }

    @InjectData
    fun doFetch(
        update: Update,
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
                parameters,
                userInfo
            )

            CallbackCommands.USER_DECLINE.isMatch(callbackData) -> onUserDecline(
                callbackMessage,
                chatId,
                userInfo,
                parameters,
            )

            CallbackCommands.USER_WITHOUT_GROUP.isMatch(callbackData) -> onUserWithoutGroup(
                callbackMessage,
                chatId,
                userInfo,
            )

            else -> userInfo
        }
    }

    private fun onUserDecline(
        message: Message,
        chat: String,
        userInfo: UserActualizedInfo,
        parameter: String?,
    ): UserActualizedInfo {
        var user = userInfo

        parameter?.let {
            when (it) {
                "fullName" -> {
                    messageSenderService.sendMessage(
                        MessageParams(
                            chatId = chat,
                            text = RegistrationMessageText.FullNameRequest(" заново")
                        )
                    )
                    user = user.copy(
                        lastUserActionType = LastUserActionType.REGISTRATION_ENTER_FULL_NAME,
                    )
                }

                "studyGroup" -> {
                    messageSenderService.sendMessage(
                        MessageParams(
                            chatId = chat,
                            text = RegistrationMessageText.GroupRequest(" заново"),
                            replyMarkup = userWithoutGroupActionCallback()
                        )
                    )
                    user = user.copy(
                        lastUserActionType = LastUserActionType.REGISTRATION_ENTER_GROUP,
                    )
                }

                else -> {}
            }
            messageSenderService.editMessage(
                MessageParams(
                    chatId = chat,
                    messageId = message.messageId,
                    text = message.text
                )
            )
        }
        return user
    }

    private fun onUserConfirm(
        message: Message,
        chat: String,
        parameter: String?,
        userInfo: UserActualizedInfo
    ): UserActualizedInfo {
        var user = userInfo

        parameter?.let {
            when (it) {
                "fullName" -> {
                    messageSenderService.sendMessage(
                        MessageParams(
                            chatId = chat,
                            text = RegistrationMessageText.GroupRequest(),
                            replyMarkup = userWithoutGroupActionCallback()
                        )
                    )
                    user = user.copy(
                        lastUserActionType = LastUserActionType.REGISTRATION_ENTER_GROUP,
                        fullName = userInfo.data,
                        data = null,
                    )
                }

                "studyGroup" -> {
                    // проставляем клавиатуру и отправляем сообщение
                    user = user.copy(
                        lastUserActionType = LastUserActionType.DEFAULT,
                        studyGroup = userInfo.data,
                        data = null,
                        isRegistered = true
                    )
                    switchKeyboardService.switchKeyboard(
                        userId = user.id!!,
                        newKeyboardType = UserKeyboardType.DEFAULT_MAIN_BOT
                    )
                    messageSenderService.sendMessage(
                        messageParams = MessageParams(
                            chatId = chat,
                            text = RegistrationMessageText.RegistrationComplete(),
                        )
                    )
                }

                else -> {}
            }

            messageSenderService.deleteMessage(
                MessageParams(
                    chatId = chat,
                    messageId = message.messageId
                )
            )
        }
        return user
    }

    private fun onUserWithoutGroup(
        message: Message,
        chat: String,
        userInfo: UserActualizedInfo,
    ): UserActualizedInfo {
        messageSenderService.sendMessage(
            MessageParams(
                chatId = chat,
                text = RegistrationMessageText.WithoutGroupConfirmation(),
                replyMarkup = createActionsKeyboard("studyGroup")
            )
        )
        messageSenderService.editMessage(
            MessageParams(
                chatId = chat,
                messageId = message.messageId,
                text = message.text
            )
        )
        return userInfo.copy(lastUserActionType = LastUserActionType.REGISTRATION_CONFIRM_GROUP)
    }
}
