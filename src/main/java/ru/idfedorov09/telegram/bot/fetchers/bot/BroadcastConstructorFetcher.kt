package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.TextCommands.BROADCAST_CONSTRUCTOR
import ru.idfedorov09.telegram.bot.data.model.CallbackData
import ru.idfedorov09.telegram.bot.data.model.ConstructorData
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.idfedorov09.telegram.bot.repo.ConstructorDataRepository
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 * Конструктор рассылки
 * bc - Broadcast Constructor
 */
@Component
class BroadcastConstructorFetcher(
    private val callbackDataRepository: CallbackDataRepository,
    private val constructorDataRepository: ConstructorDataRepository,
) : GeneralFetcher() {

    companion object {
        /** Максимальное кол-во кнопок в рассылке **/
        const val MAX_BUTTONS_COUNT = 5
    }

    @InjectData
    fun doFetch(
        userActualizedInfo: UserActualizedInfo,
        update: Update,
        bot: Executor,
    ) {
        val params = Params(
            bot,
            userActualizedInfo,
        )

        when {
            update.hasMessage() && update.message.hasText() -> textCommandsHandler(update, params)
            update.hasCallbackQuery() -> callbackQueryHandler(update, params)
        }
    }

    private fun textCommandsHandler(update: Update, params: Params) {
        val text = update.message.text

        text.apply {
            when {
                startsWith(BROADCAST_CONSTRUCTOR()) -> showConsole(params)
            }
        }
    }

    private fun callbackQueryHandler(update: Update, params: Params) {
        update.callbackQuery.data.apply {
            when {
                startsWith("#bc_cancel") -> bcCancel(params)
            }
        }
    }

    private fun bcCancel(params: Params) {
        params.userActualizedInfo.apply {
            constructorData ?: return
            constructorData?.lastConsoleId ?: return

            params.bot.execute(
                DeleteMessage().also {
                    it.chatId = tui
                    it.messageId = constructorData?.lastConsoleId?.toInt()!!
                }
            )
            constructorData = null
        }
    }

    private fun showConsole(params: Params) {
        val constructorData = params.userActualizedInfo.constructorData
        if (constructorData == null) {
            params.userActualizedInfo.constructorData = constructorDataRepository.save(
                ConstructorData(authorTui = params.userActualizedInfo.tui)
            )
            val messageText = "<b>Конструктор рассылки</b>\n\nВыберите дальнейшее действие"
            val newPhoto = CallbackData(callbackData = "#bc_change_photo", metaText = "Добавить фото").save()
            val addText = CallbackData(callbackData = "#bc_change_text", metaText = "Добавить текст").save()
            val addButton = CallbackData(callbackData = "#bc_add_button", metaText = "Добавить кнопку").save()
            val cancelButton = CallbackData(callbackData = "#bc_cancel", metaText = "Отмена").save()

            val keyboard =
                listOf(newPhoto, addText, addButton, cancelButton).map { button ->
                    InlineKeyboardButton().also {
                        it.text = button.metaText!!
                        it.callbackData = button.callbackData
                    }
                }.map { listOf(it) }

            val sent = params.bot.execute(
                SendMessage().also {
                    it.text = messageText
                    it.parseMode = ParseMode.HTML
                    it.replyMarkup = createKeyboard(keyboard)
                    it.chatId = params.userActualizedInfo.tui
                },
            )

            params.userActualizedInfo.constructorData = params.userActualizedInfo.constructorData?.copy(
                lastConsoleId = sent.messageId?.toString()
            )
            return
        } else {
            val photoProp = CallbackData(
                callbackData = "#bc_change_photo",
                metaText = constructorData.photoId?.let { "Изменить фото" } ?: "Добавить фото"
            ).save()
            val textProp = CallbackData(
                callbackData = "#bc_change_text",
                metaText = constructorData.text?.let { "Изменить текст" } ?: "Добавить текст"
            ).save()
            val addButton = CallbackData(callbackData = "#bc_add_button", metaText = "Добавить кнопку").save()

            val changeDate = CallbackData(callbackData = "#bc_change_date", metaText = "Добавить кнопку").save()
            val previewButton = CallbackData(callbackData = "#bc_add_button", metaText = "Предпросмотр").save()
            val cancelButton = CallbackData(callbackData = "#bc_cancel", metaText = "Отмена").save()

            val keyboard = mutableListOf(
                photoProp, textProp, addButton, changeDate, previewButton, cancelButton
            ).apply {
                // TODO: если кол-во кнопок >=5 то убрать кнопку 'добавление кнопки'
            }.map {  callbackData ->
                listOf(InlineKeyboardButton().also { it.text = callbackData.metaText!! })
            }

            val text = constructorData.run {
                val title = "<b>Конструктор рассылки</b>\n\n"
                val text = text?.let { "Текст:\n<pre>${text}</pre>\n" } ?: ""
                val end = "Выберите дальнейшее действие"
                title+text+end
            }

            // TODO: кнопки

            params.bot.execute(
                DeleteMessage().also {
                    it.chatId = params.userActualizedInfo.tui
                    it.messageId = constructorData.lastConsoleId?.toInt()!!
                }
            )

            when (constructorData.photoId) {
                null -> params.bot.execute(
                    SendMessage().also {
                        it.chatId = params.userActualizedInfo.tui
                        it.text = text
                        it.replyMarkup = createKeyboard(keyboard)
                    }
                )
                else -> params.bot.execute(
                    SendPhoto().also {
                        it.chatId = params.userActualizedInfo.tui
                        it.caption = text
                        it.replyMarkup = createKeyboard(keyboard)
                        it.photo = InputFile(constructorData.photoId)
                    }
                )
            }

        }
    }

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>)
        = InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun CallbackData.save() = callbackDataRepository.save(this)

    private data class Params(
        val bot: Executor,
        val userActualizedInfo: UserActualizedInfo,
    )
}
