package ru.idfedorov09.telegram.bot.fetchers.bot.userfetchers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.RegistrationMessageText
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

@Component
class RegistrationFetcher(
    private val updatesUtil: UpdatesUtil,
    private val userRepository: UserRepository,
    private val messageSenderService: MessageSenderService,
) : GeneralFetcher() {
    companion object {
        private val log = LoggerFactory.getLogger(RegistrationFetcher::class.java)
    }

    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ): UserActualizedInfo {
        val chatId = updatesUtil.getChatId(update) ?: return userActualizedInfo
        val message = update.message.takeIf { update.hasMessage() } ?: return userActualizedInfo

        val action =
            userActualizedInfo.lastUserActionType ?: run {
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = chatId,
                        text = RegistrationMessageText.RegistrationStart(),
                    ),
                )
                LastUserActionType.REGISTRATION_START
            }

        var userInfo = userActualizedInfo

        when (action) {
            LastUserActionType.REGISTRATION_START -> {
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = chatId,
                        text = RegistrationMessageText.FullNameRequest(),
                    ),
                )
                userInfo =
                    userInfo.copy(
                        lastUserActionType = LastUserActionType.REGISTRATION_ENTER_FULL_NAME,
                    )
            }

            LastUserActionType.REGISTRATION_ENTER_FULL_NAME -> {
                if (message.text.isValidFullName()) {
                    messageSenderService.sendMessage(
                        MessageParams(
                            chatId = chatId,
                            text = RegistrationMessageText.FullNameConfirmation.format(message.text),
                            replyMarkup = createActionsKeyboard("fullName"),
                        ),
                    )
                    userInfo =
                        userInfo.copy(
                            lastUserActionType = LastUserActionType.REGISTRATION_CONFIRM_FULL_NAME,
                            data = message.text,
                        )
                } else {
                    messageSenderService.sendMessage(
                        MessageParams(
                            chatId = chatId,
                            text = RegistrationMessageText.InvalidFullName(),
                        ),
                    )
                }
            }

            LastUserActionType.REGISTRATION_ENTER_GROUP -> {
                userRepository.findByFullNameAndStudyGroup(userInfo.fullName ?: "", message.text)?.run {
                    messageSenderService.sendMessage(
                        MessageParams(
                            chatId = chatId,
                            text = RegistrationMessageText.AlreadyExists.format(userInfo.lastTgNick ?: ""),
                        ),
                    )
                    return userInfo
                }
                if (message.text.isValidGroup()) {
                    messageSenderService.sendMessage(
                        MessageParams(
                            chatId = chatId,
                            text = RegistrationMessageText.GroupConfirmation.format(message.text),
                            replyMarkup = createActionsKeyboard("studyGroup"),
                        ),
                    )
                    userInfo =
                        userInfo.copy(
                            lastUserActionType = LastUserActionType.REGISTRATION_CONFIRM_GROUP,
                            data = message.text.uppercase(),
                        )
                } else {
                    messageSenderService.sendMessage(
                        MessageParams(
                            chatId = chatId,
                            text = RegistrationMessageText.InvalidGroup(),
                        ),
                    )
                }
            }

            else -> {
                if (action != LastUserActionType.REGISTRATION_CONFIRM_GROUP &&
                    action != LastUserActionType.REGISTRATION_CONFIRM_FULL_NAME
                ) {
                    userInfo = userInfo.copy(isRegistered = true)
                }
            }
        }

        return userInfo
    }

    private fun String?.isValidFullName() =
        this?.let {
            it.isNotEmpty() && it.length < 80
        } ?: false

    private fun String?.isValidGroup() =
        this?.let {
            it.isNotEmpty() && "([АМСБамсб]{1})([0-9]{2})-([0-9]{3})".toRegex().matches(it)
        } ?: false
}
