package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.RegistrationMessageText
import ru.idfedorov09.telegram.bot.data.enums.UserKeyboardType
import ru.idfedorov09.telegram.bot.data.model.CallbackData
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.idfedorov09.telegram.bot.service.SwitchKeyboardService
import ru.mephi.sno.libs.flow.belly.InjectData
import kotlin.jvm.optionals.getOrNull

@Component
class RegistrationFetcher(
    private val callbackDataRepository: CallbackDataRepository,
    private val messageSenderService: MessageSenderService,
    private val userRepository: UserRepository,
    private val switchKeyboardService: SwitchKeyboardService,
) : DefaultFetcher() {
    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ): UserActualizedInfo {
        val params =
            Params(
                userActualizedInfo,
                update,
            )
        return when {
            update.hasMessage() && update.message.hasText() -> textCommandsHandler(params)
            update.hasCallbackQuery() -> callbackQueryHandler(params)
            else -> userActualizedInfo
        }
    }

    private fun textCommandsHandler(params: Params): UserActualizedInfo {
        params.userActualizedInfo.apply {
            return when (lastUserActionType) {
                LastUserActionType.REGISTRATION_START -> registrationStart(params)
                LastUserActionType.REGISTRATION_ENTER_FULL_NAME -> enterFullName(params)
                LastUserActionType.REGISTRATION_ENTER_GROUP -> enterStudyGroup(params)
                else -> this
            }
        }
    }

    private fun registrationStart(params: Params): UserActualizedInfo {
        params.userActualizedInfo.apply {
            val sendMessage =
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = tui,
                        text = RegistrationMessageText.RegistrationStart(),
                    ),
                )

            lastUserActionType = LastUserActionType.REGISTRATION_ENTER_FULL_NAME
            data = sendMessage.messageId.toString()
            return this
        }
    }

    private fun enterFullName(params: Params): UserActualizedInfo {
        params.userActualizedInfo.apply {
            messageSenderService.deleteMessage(
                MessageParams(
                    chatId = tui,
                    messageId = params.update.message.messageId,
                ),
            )

            if (data?.let { Regex("\\d+").matches(it) } == true) {
                messageSenderService.deleteMessage(
                    MessageParams(
                        chatId = tui,
                        messageId = data!!.toInt(),
                    ),
                )
                data = null
            }

            if (params.update.message?.text.isValidFullName()) {
                val confirm =
                    CallbackData(
                        metaText = "✅ Подтвердить",
                        callbackData = CallbackCommands.REGISTRATION_CONFIRM_FULL_NAME.data,
                    ).save()
                val cancel =
                    CallbackData(
                        metaText = "❌ Отменить",
                        callbackData = CallbackCommands.REGISTRATION_DECLINE_FULL_NAME.data,
                    ).save()
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = tui,
                        text = RegistrationMessageText.FullNameConfirmation.format(params.update.message.text),
                        replyMarkup = createActionsKeyboard(confirm, cancel),
                    ),
                )
                lastUserActionType = LastUserActionType.REGISTRATION_CONFIRM_FULL_NAME
                data = params.update.message.text
            } else {
                val sendMessage =
                    messageSenderService.sendMessage(
                        MessageParams(
                            chatId = tui,
                            text = RegistrationMessageText.InvalidFullName(),
                        ),
                    )
                data = sendMessage.messageId.toString()
            }
            return this
        }
    }

    private fun enterStudyGroup(params: Params): UserActualizedInfo {
        params.userActualizedInfo.apply {
            messageSenderService.deleteMessage(
                MessageParams(
                    chatId = tui,
                    messageId = params.update.message.messageId,
                ),
            )

            if (data?.let { Regex("\\d+").matches(it) } == true) {
                messageSenderService.deleteMessage(
                    MessageParams(
                        chatId = tui,
                        messageId = data!!.toInt(),
                    ),
                )
                data = null
            }

            if (params.update.message.text.isValidGroup()) {
                val confirm =
                    CallbackData(
                        metaText = "✅ Подтвердить",
                        callbackData = CallbackCommands.REGISTRATION_CONFIRM_STUDY_GROUP.data,
                    ).save()
                val cancel =
                    CallbackData(
                        metaText = "❌ Отменить",
                        callbackData = CallbackCommands.REGISTRATION_DECLINE_STUDY_GROUP.data,
                    ).save()

                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = tui,
                        text = RegistrationMessageText.GroupConfirmation.format(params.update.message.text),
                        replyMarkup = createActionsKeyboard(confirm, cancel),
                    ),
                )
                lastUserActionType = LastUserActionType.REGISTRATION_CONFIRM_GROUP
                data = params.update.message.text.uppercase()
            } else {
                val withoutGroup =
                    CallbackData(
                        metaText = "👾Я не из МИФИ",
                        callbackData = CallbackCommands.REGISTRATION_WITHOUT_STUDY_GROUP.data,
                    ).save()
                val sendMessage =
                    messageSenderService.sendMessage(
                        MessageParams(
                            chatId = tui,
                            text = RegistrationMessageText.InvalidGroup(),
                            replyMarkup = createActionsKeyboard(withoutGroup),
                        ),
                    )
                data = sendMessage.messageId.toString()
            }
            return this
        }
    }

    private fun callbackQueryHandler(params: Params): UserActualizedInfo {
        params.apply {
            val callbackId = update.callbackQuery.data?.toLongOrNull()
            callbackId ?: return params.userActualizedInfo
            val callbackData = callbackDataRepository.findById(callbackId).getOrNull() ?: return params.userActualizedInfo

            return callbackData.callbackData?.run {
                when {
                    startsWith(CallbackCommands.REGISTRATION_CONFIRM_FULL_NAME.data) -> confirmFullName(params)
                    startsWith(CallbackCommands.REGISTRATION_DECLINE_FULL_NAME.data) -> declineFullName(params)
                    startsWith(CallbackCommands.REGISTRATION_CONFIRM_STUDY_GROUP.data) -> confirmStudyGroup(params)
                    startsWith(CallbackCommands.REGISTRATION_DECLINE_STUDY_GROUP.data) -> declineStudyGroup(params)
                    startsWith(CallbackCommands.REGISTRATION_WITHOUT_STUDY_GROUP.data) -> withoutStudyGroup(params)
                    else -> params.userActualizedInfo
                }
            } ?: userActualizedInfo
        }
    }

    private fun withoutStudyGroup(params: Params): UserActualizedInfo {
        params.userActualizedInfo.apply {
            val confirm =
                CallbackData(
                    metaText = "✅ Подтвердить",
                    callbackData = CallbackCommands.REGISTRATION_CONFIRM_STUDY_GROUP.data,
                ).save()
            val cancel =
                CallbackData(
                    metaText = "❌ Отменить",
                    callbackData = CallbackCommands.REGISTRATION_DECLINE_STUDY_GROUP.data,
                ).save()
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = tui,
                    text = RegistrationMessageText.WithoutGroupConfirmation(),
                    replyMarkup = createActionsKeyboard(confirm, cancel),
                ),
            )
            messageSenderService.deleteMessage(
                MessageParams(
                    chatId = tui,
                    messageId = params.update.callbackQuery.message.messageId,
                    text = params.update.callbackQuery.message.text,
                ),
            )
            lastUserActionType = LastUserActionType.REGISTRATION_CONFIRM_GROUP
            return this
        }
    }

    private fun declineStudyGroup(params: Params): UserActualizedInfo {
        params.userActualizedInfo.apply {
            val withoutGroup =
                CallbackData(
                    metaText = "👾Я не из МИФИ",
                    callbackData = CallbackCommands.REGISTRATION_WITHOUT_STUDY_GROUP.data,
                ).save()

            val sendMessage =
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = tui,
                        text = RegistrationMessageText.GroupRequest(" заново"),
                        replyMarkup = createActionsKeyboard(withoutGroup),
                    ),
                )
            lastUserActionType = LastUserActionType.REGISTRATION_ENTER_GROUP
            data = sendMessage.messageId.toString()
            messageSenderService.deleteMessage(
                MessageParams(
                    chatId = tui,
                    messageId = params.update.callbackQuery.message.messageId,
                ),
            )
            return this
        }
    }

    private fun confirmStudyGroup(params: Params): UserActualizedInfo {
        params.userActualizedInfo.apply {
            params.userActualizedInfo =
                params.userActualizedInfo.copy(
                    lastUserActionType = LastUserActionType.DEFAULT,
                    studyGroup = data,
                    data = null,
                    isRegistered = true,
                )
            switchKeyboardService.switchKeyboard(
                userId = id!!,
                newKeyboardType = UserKeyboardType.DEFAULT_MAIN_BOT,
            )
            userRepository.updateUserCategoriesById(userId = id!!)
            messageSenderService.sendMessage(
                messageParams =
                    MessageParams(
                        chatId = tui,
                        text = RegistrationMessageText.RegistrationComplete(),
                    ),
            )
            messageSenderService.deleteMessage(
                MessageParams(
                    chatId = tui,
                    messageId = params.update.callbackQuery.message.messageId,
                ),
            )
            return params.userActualizedInfo
        }
    }

    private fun declineFullName(params: Params): UserActualizedInfo {
        params.userActualizedInfo.apply {
            val sendMessage =
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = tui,
                        text = RegistrationMessageText.FullNameRequest(" заново"),
                    ),
                )
            lastUserActionType = LastUserActionType.REGISTRATION_ENTER_FULL_NAME
            data = sendMessage.messageId.toString()
            messageSenderService.deleteMessage(
                MessageParams(
                    chatId = tui,
                    messageId = params.update.callbackQuery.message.messageId,
                ),
            )
            return this
        }
    }

    private fun confirmFullName(params: Params): UserActualizedInfo {
        params.userActualizedInfo.apply {
            val withoutGroup =
                CallbackData(
                    metaText = "👾Я не из МИФИ",
                    callbackData = CallbackCommands.REGISTRATION_WITHOUT_STUDY_GROUP.data,
                ).save()
            val sendMessage =
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = tui,
                        text = RegistrationMessageText.GroupRequest(),
                        replyMarkup = createActionsKeyboard(withoutGroup),
                    ),
                )
            messageSenderService.deleteMessage(
                MessageParams(
                    chatId = tui,
                    messageId = params.update.callbackQuery.message.messageId,
                ),
            )
            params.userActualizedInfo =
                params.userActualizedInfo.copy(
                    lastUserActionType = LastUserActionType.REGISTRATION_ENTER_GROUP,
                    fullName = data,
                    data = sendMessage.messageId.toString(),
                )
            return params.userActualizedInfo
        }
    }

    private fun String?.isValidFullName() =
        this?.let {
            it.isNotEmpty() && it.length < 80
        } ?: false

    private fun String?.isValidGroup() =
        this?.let {
            it.isNotEmpty() && "([АМСБамсб]{1})([0-9]{2})-([0-9]{3})".toRegex().matches(it)
        } ?: false

    private fun createActionsKeyboard(
        firstCallbackData: CallbackData,
        secondCallbackData: CallbackData,
    ) = InlineKeyboardMarkup(
        listOf(
            listOf(
                InlineKeyboardButton(firstCallbackData.metaText!!).also {
                    it.callbackData = firstCallbackData.id.toString()
                },
                InlineKeyboardButton(secondCallbackData.metaText!!).also {
                    it.callbackData = secondCallbackData.id.toString()
                },
            ),
        ),
    )

    private fun createActionsKeyboard(callbackData: CallbackData) =
        InlineKeyboardMarkup(
            listOf(
                listOf(
                    InlineKeyboardButton(callbackData.metaText!!).also {
                        it.callbackData = callbackData.id.toString()
                    },
                ),
            ),
        )

    private fun CallbackData.save() = callbackDataRepository.save(this)

    private data class Params(
        var userActualizedInfo: UserActualizedInfo,
        val update: Update,
    )
}
