package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.CallbackData
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData
import kotlin.jvm.optionals.getOrNull

@Component
class UserSettingFetcher(
    private val callbackDataRepository: CallbackDataRepository,
    private val messageSenderService: MessageSenderService,
) : DefaultFetcher() {

    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ): UserActualizedInfo {
        val params = Params(userActualizedInfo, update)
        return when {
            update.hasMessage() && update.message.hasText() -> textCommandsHandler(params)
            update.hasCallbackQuery() -> callbackQueryHandler(params)
            else -> userActualizedInfo
        }
    }

    private fun textCommandsHandler(params: Params): UserActualizedInfo {
        val text = params.update.message.text
        text.apply {
            return when {
                startsWith(TextCommands.USER_SETTING_REPLY()) -> showUserInfo(params)
                else -> commonTextHandler(params)
            }
        }
    }

    private fun commonTextHandler(params: Params): UserActualizedInfo {
        return when (params.userActualizedInfo.lastUserActionType) {
            LastUserActionType.SETTING_USER_ENTER_FULL_NAME -> enterFullName(params)
            LastUserActionType.SETTING_USER_ENTER_STUDY_GROUP -> enterStudyGroup(params)
            else -> params.userActualizedInfo
        }
    }

    private fun showUserInfo(params: Params): UserActualizedInfo {
        params.apply {
            val text =
                "Информация о вашем аккаунте:\n\n" +
                    "Ваше ФИО: <code>${userActualizedInfo.fullName ?: "\uFE0F ИНФОРМАЦИЯ НЕ НАЙДЕНА"}</code>\n" +
                    "Ваше СНО/СМУС: <code>${userActualizedInfo.snoName ?: "\uFE0F ИНФОРМАЦИЯ НЕ НАЙДЕНА"}</code>\n\n" +
                    "Если эта информация неверна или неактуальна, то вы можете ее изменить."
            val changeFullName =
                CallbackData(
                    callbackData = CallbackCommands.SETTING_USER_CHANGE_FULL_NAME.data,
                    metaText = "Обновить ФИО",
                ).save()
            val changeStudyGroup =
                CallbackData(
                    callbackData = CallbackCommands.SETTING_USER_CHANGE_STUDY_GROUP.data,
                    metaText = "Обновить СНО/СМУС",
                ).save()
            val sentMessage =
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = userActualizedInfo.tui,
                        text = text,
                        replyMarkup = createKeyboard(changeFullName, changeStudyGroup),
                        parseMode = ParseMode.HTML
                    ),
                )
            if (userActualizedInfo.data?.userSettingMessageId != null) {
                messageSenderService.deleteMessage(
                    MessageParams(
                        chatId = userActualizedInfo.tui,
                        messageId = userActualizedInfo.data?.userSettingMessageId,
                    ),
                )
            }
            userActualizedInfo.data?.userSettingMessageId = sentMessage.messageId
            return userActualizedInfo
        }
    }

    private fun enterFullName(params: Params): UserActualizedInfo {
        params.apply {
            val msgText = update.message.text
            if (msgText.isValidFullName()) {
                val repeat =
                    CallbackData(
                        callbackData = CallbackCommands.SETTING_USER_CHANGE_FULL_NAME.data,
                        metaText = "Изменить ФИО снова",
                    ).save()
                val back =
                    CallbackData(
                        callbackData = CallbackCommands.SETTING_USER_BACK_TO_CONSOLE.data,
                        metaText = "Вернуться назад",
                    ).save()
                messageSenderService.editMessage(
                    MessageParams(
                        chatId = userActualizedInfo.tui,
                        messageId = userActualizedInfo.data?.userSettingMessageId,
                        text = "Ваше ФИО изменено на $msgText",
                        replyMarkup = createKeyboard(repeat, back),
                    ),
                )
                userActualizedInfo =
                    userActualizedInfo.copy(
                        fullName = msgText,
                        lastUserActionType = LastUserActionType.DEFAULT,
                    )
            } else {
                messageSenderService.editMessage(
                    MessageParams(
                        chatId = userActualizedInfo.tui,
                        messageId = userActualizedInfo.data?.userSettingMessageId,
                        text = "Кажется Вы ввели ФИО неправильно. Используйте только символы из кириллицы и пробелы",
                    ),
                )
            }
            deleteUpdateMessage()
            return userActualizedInfo
        }
    }

    private fun enterStudyGroup(params: Params): UserActualizedInfo {
        params.apply {
            val msgText = update.message.text
            val repeat =
                CallbackData(
                    callbackData = CallbackCommands.SETTING_USER_CHANGE_FULL_NAME.data,
                    metaText = "Изменить СНО/СМУС снова",
                ).save()
            val back =
                CallbackData(
                    callbackData = CallbackCommands.SETTING_USER_BACK_TO_CONSOLE.data,
                    metaText = "Вернуться назад",
                ).save()
            messageSenderService.editMessage(
                MessageParams(
                    chatId = userActualizedInfo.tui,
                    messageId = userActualizedInfo.data?.userSettingMessageId,
                    text = "Название вашего СНО/СМУС изменено на $msgText",
                    replyMarkup = createKeyboard(repeat, back),
                ),
            )
            userActualizedInfo =
                userActualizedInfo.copy(
                    snoName = msgText,
                    lastUserActionType = LastUserActionType.DEFAULT,
                )
            deleteUpdateMessage()
            return userActualizedInfo
        }
    }

    private fun callbackQueryHandler(params: Params): UserActualizedInfo {
        val callbackId = params.update.callbackQuery.data?.toLongOrNull()
        callbackId ?: return params.userActualizedInfo
        val callbackData = callbackDataRepository.findById(callbackId).getOrNull() ?: return params.userActualizedInfo

        return callbackData.callbackData?.run {
            when {
                CallbackCommands.SETTING_USER_CHANGE_FULL_NAME.isMatch(this) -> changeFullName(params)
                CallbackCommands.SETTING_USER_CHANGE_STUDY_GROUP.isMatch(this) -> changeStudyGroup(params)
                CallbackCommands.SETTING_USER_WITHOUT_STUDY_GROUP.isMatch(this) -> withoutStudyGroup(params)
                CallbackCommands.SETTING_USER_BACK_TO_CONSOLE.isMatch(this) -> showUserInfo(params)
                else -> params.userActualizedInfo
            }
        } ?: params.userActualizedInfo
    }

    private fun withoutStudyGroup(params: Params): UserActualizedInfo {
        params.apply {
            val repeat =
                CallbackData(
                    callbackData = CallbackCommands.SETTING_USER_CHANGE_FULL_NAME.data,
                    metaText = "Изменить СНО/СМУС снова",
                ).save()
            val back =
                CallbackData(
                    callbackData = CallbackCommands.SETTING_USER_BACK_TO_CONSOLE.data,
                    metaText = "Вернуться назад",
                ).save()
            messageSenderService.editMessage(
                MessageParams(
                    chatId = userActualizedInfo.tui,
                    messageId = userActualizedInfo.data?.userSettingMessageId,
                    text = "Ваш номер группы изменен на Не из МИФИ",
                    replyMarkup = createKeyboard(repeat, back),
                ),
            )
            userActualizedInfo =
                userActualizedInfo.copy(
                    snoName = "Не из МИФИ",
                    lastUserActionType = LastUserActionType.DEFAULT,
                )
            return userActualizedInfo
        }
    }

    private fun changeStudyGroup(params: Params): UserActualizedInfo {
        params.apply {
            val text = "Введите название вашего СНО/СМУС"
            messageSenderService.editMessage(
                MessageParams(
                    chatId = userActualizedInfo.tui,
                    messageId = userActualizedInfo.data?.userSettingMessageId,
                    text = text,
                ),
            )
            userActualizedInfo.lastUserActionType = LastUserActionType.SETTING_USER_ENTER_STUDY_GROUP
            return userActualizedInfo
        }
    }

    private fun changeFullName(params: Params): UserActualizedInfo {
        params.apply {
            val text = "Введите свое ФИО"
            messageSenderService.editMessage(
                MessageParams(
                    chatId = userActualizedInfo.tui,
                    messageId = userActualizedInfo.data?.userSettingMessageId,
                    text = text,
                ),
            )
            userActualizedInfo.lastUserActionType = LastUserActionType.SETTING_USER_ENTER_FULL_NAME
            return userActualizedInfo
        }
    }

    private fun createKeyboard(vararg callbackData: CallbackData): InlineKeyboardMarkup {
        val keyboard =
            listOf(*callbackData).map { button ->
                InlineKeyboardButton().also {
                    it.text = button.metaText!!
                    it.callbackData = button.id?.toString()
                }
            }.map { listOf(it) }
        return createKeyboard(keyboard)
    }

    private fun String?.isValidFullName() =
        this?.let {
            it.isNotEmpty() && it.length < 80
        } ?: false

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun CallbackData.save() = callbackDataRepository.save(this)

    private data class Params(
        var userActualizedInfo: UserActualizedInfo,
        val update: Update,
    )
}
