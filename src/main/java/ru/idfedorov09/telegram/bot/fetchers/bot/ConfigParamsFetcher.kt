package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.base.executor.Executor
import ru.idfedorov09.telegram.bot.base.util.UpdatesUtil
import ru.idfedorov09.telegram.bot.data.enums.ConfigParamType
import ru.idfedorov09.telegram.bot.data.enums.ConfigParams
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.CallbackData
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.data.model.UserData
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.idfedorov09.telegram.bot.service.ConfigParamsService
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData
import kotlin.jvm.optionals.getOrNull

@Component
class ConfigParamsFetcher(
    private val callbackDataRepository: CallbackDataRepository,
    private val messageSenderService: MessageSenderService,
    private val updatesUtil: UpdatesUtil,
    private val bot: Executor,
    private val configParamsService: ConfigParamsService,
) : DefaultFetcher() {

    companion object {
        private const val SEPARATOR = "04b6c5cb-2519-4d02-bc4b-6c7e3ddf5764"
        private const val CONFIGURE_PARAM_PREFIX = "configure_parameter"
    }

    @InjectData
    fun doFetch(update: Update, userActualizedInfo: UserActualizedInfo): UserActualizedInfo {
        return when {
            update.hasMessage() && update.message.hasText() -> textCommandsHandler(update, userActualizedInfo)
            update.hasCallbackQuery() -> callbackQueryHandler(update, userActualizedInfo)
            else -> userActualizedInfo
        }
    }

    private fun textCommandsHandler(update: Update, userActualizedInfo: UserActualizedInfo): UserActualizedInfo {
        val text = update.message.text

        text.apply {
            return when {
                startsWith(TextCommands.CONFIG_PARAMS()) -> showConfigParams(update, userActualizedInfo)
                else -> commonTextHandler(update, userActualizedInfo)
            }
        }
    }

    private fun commonTextHandler(update: Update, userActualizedInfo: UserActualizedInfo): UserActualizedInfo {
        return when (userActualizedInfo.lastUserActionType) {
            LastUserActionType.INPUT_CONFIGURE_PARAMETER_VALUE -> changeConfig(update, userActualizedInfo)
            else -> userActualizedInfo
        }
    }

    private fun callbackQueryHandler(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ): UserActualizedInfo {
        val callbackId = update.callbackQuery.data?.toLongOrNull()
        callbackId ?: return userActualizedInfo
        val callbackData = callbackDataRepository.findById(callbackId).getOrNull() ?: return userActualizedInfo

        callbackData.callbackData?.apply {
            if (startsWith(CONFIGURE_PARAM_PREFIX))
                return configureParameterResolveByType(update, userActualizedInfo, this)
        }
        return userActualizedInfo
    }

    private fun changeConfig(update: Update, userActualizedInfo: UserActualizedInfo): UserActualizedInfo {
        val parameter = ConfigParams.getByKey(userActualizedInfo.data?.configParamKeyToChange)
            ?: return userActualizedInfo.copy(
                lastUserActionType = LastUserActionType.DEFAULT
            )

        val newValue = update.message.text.trim()
        configParamsService.setValue(parameter, newValue)
        deleteUpdateMessage()
        messageSenderService.sendMessage(
            MessageParams(
                chatId = updatesUtil.getChatId(update)!!,
                text = "✅ Значение параметра ${parameter.displayName} успешно изменено на <code>$newValue</code>",
                parseMode = ParseMode.HTML,
            )
        )

        return userActualizedInfo.copy(
            lastUserActionType = LastUserActionType.DEFAULT,
        )
    }

    private fun configureParameterResolveByType(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
        callbackData: String
    ): UserActualizedInfo {
        val parameter = ConfigParams.getByKey(callbackData.split(SEPARATOR).lastOrNull()) ?: run {
            val answerCallbackQuery =
                AnswerCallbackQuery().also {
                    it.callbackQueryId = update.callbackQuery.id
                    it.text = "☹\uFE0F Такого параметра не существует."
                    it.showAlert = true
                }
            bot.execute(answerCallbackQuery)
            messageSenderService.deleteMessage(
                MessageParams(
                    chatId = update.callbackQuery.message.chatId.toString(),
                    messageId = update.callbackQuery.message.messageId,
                )
            )
            return userActualizedInfo
        }

        return when (parameter.type) {
            ConfigParamType.INPUT -> processInputParam(update, userActualizedInfo, parameter)
            ConfigParamType.SELECT_ONE -> processSelectOneParam(update, userActualizedInfo, parameter)
            ConfigParamType.SELECT_MANY -> processSelectManyParam(update, userActualizedInfo, parameter)
        }
    }

    private fun processInputParam(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
        param: ConfigParams
    ): UserActualizedInfo {
        messageSenderService.editMessage(
            MessageParams(
                text = "Введите новое значение параметра ${param.displayName}",
                messageId = update.callbackQuery.message.messageId,
                chatId = update.callbackQuery.message.chatId.toString(),
            )
        )

        userActualizedInfo.apply {
            return copy(
                lastUserActionType = LastUserActionType.INPUT_CONFIGURE_PARAMETER_VALUE,
                data = data?.let { it.copy(configParamKeyToChange = param.key) } ?: UserData(
                    configParamKeyToChange = param.key
                )
            )
        }
    }

    // TODO
    private fun processSelectOneParam(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
        param: ConfigParams
    ): UserActualizedInfo {
        val answerCallbackQuery =
            AnswerCallbackQuery().also {
                it.callbackQueryId = update.callbackQuery.id
                it.text = "☹\uFE0F Функционал изменения такого типа параметров еще в разработке."
                it.showAlert = true
            }
        bot.execute(answerCallbackQuery)
        return userActualizedInfo
    }

    // TODO
    private fun processSelectManyParam(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
        param: ConfigParams
    ): UserActualizedInfo {
        val answerCallbackQuery =
            AnswerCallbackQuery().also {
                it.callbackQueryId = update.callbackQuery.id
                it.text = "☹\uFE0F Функционал изменения такого типа параметров еще в разработке."
                it.showAlert = true
            }
        bot.execute(answerCallbackQuery)
        return userActualizedInfo
    }

    private fun showConfigParams(update: Update, userActualizedInfo: UserActualizedInfo): UserActualizedInfo {
        if (userActualizedInfo.lastUserActionType != LastUserActionType.DEFAULT) return userActualizedInfo

        val keyboard = ConfigParams.entries
            .filter { it.isAllowed(userActualizedInfo) }
            .map {
                CallbackData(
                    callbackData = "$CONFIGURE_PARAM_PREFIX$SEPARATOR${it.key}",
                    metaText = it.displayName,
                ).save()
            }

        val text =
            if (keyboard.isEmpty())
                "❌ Нет доступных параметров, которые Вы можете настраивать. Возможно, у Вас недостаточно доступа."
            else
                "Выберите параметры, которые Вы хотите настроить"

        messageSenderService.sendMessage(
            MessageParams(
                chatId = updatesUtil.getChatId(update)!!,
                text = text,
                replyMarkup = createKeyboard(*keyboard.toTypedArray()),
            )
        )
        return userActualizedInfo
    }

    private fun createKeyboard(vararg callbackData: CallbackData): InlineKeyboardMarkup {
        val keyboard =
            listOf(*callbackData).map { button ->
                InlineKeyboardButton().also {
                    it.text = button.metaText!!
                    it.callbackData = button.id?.toString()
                    it.url = button.metaUrl
                }
            }.map { listOf(it) }
        return createKeyboard(keyboard)
    }

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }
    private fun CallbackData.save() = callbackDataRepository.save(this)
}