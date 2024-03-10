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
import ru.idfedorov09.telegram.bot.data.GlobalConstants.BOT_TIME_ZONE
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.TextCommands.BROADCAST_CONSTRUCTOR
import ru.idfedorov09.telegram.bot.data.model.Broadcast
import ru.idfedorov09.telegram.bot.data.model.CallbackData
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

/**
 * Конструктор рассылки
 * bc - Broadcast Constructor
 */
@Component
class BroadcastConstructorFetcher(
    private val callbackDataRepository: CallbackDataRepository,
    private val broadcastRepository: BroadcastRepository,
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
            update,
        )

        when {
            update.hasMessage() && update.message.hasText() -> textCommandsHandler(update, params)
            update.hasCallbackQuery() -> callbackQueryHandler(update, params)
            update.hasMessage() && update.message.hasPhoto() -> photoHandler(update, params)
        }
    }

    private fun textCommandsHandler(update: Update, params: Params) {
        val text = update.message.text

        text.apply {
            when {
                startsWith(BROADCAST_CONSTRUCTOR()) -> showBcConsole(params)
                else -> commonTextHandler(params)
            }
        }
    }

    /**
     * Обработка обычного текста (ввод текста рассылки, ссылок и тд и тп)
     */
    private fun commonTextHandler(params: Params) {
        when (params.userActualizedInfo.lastUserActionType) {
            LastUserActionType.BC_TEXT_TYPE -> changeText(params)
            else -> return
        }
    }

    private fun callbackQueryHandler(update: Update, params: Params) {
        val callbackId = update.callbackQuery.data?.toLongOrNull()
        callbackId ?: return
        val callbackData = callbackDataRepository.findById(callbackId).getOrNull() ?: return

        callbackData.callbackData?.apply {
            when {
                startsWith("#bc_cancel") -> bcCancel(params)
                startsWith("#bc_change_text") -> bcChangeTextMessage(params)
                startsWith("#bc_change_photo") -> bcChangePhoto(params)
                startsWith("#bc_action_cancel") -> bcCancelAction(params)
                startsWith("#bc_preview") -> bcPreview(params)
                startsWith("#bc_send_now") -> bcSendNow(params)
            }
        }
    }

    private fun photoHandler(update: Update, params: Params) {
        when (params.userActualizedInfo.lastUserActionType) {
            LastUserActionType.BC_PHOTO_TYPE -> changePhoto(params)
            else -> return
        }
    }

    private fun changeText(params: Params) {
        if (params.userActualizedInfo.bcData?.imageHash != null && params.update.message.text.length > 1024) {
            params.bot.execute(
                SendMessage().also {
                    it.text = "Ошибка! Невозможно добавить текст длины" +
                            "${params.userActualizedInfo.bcData?.text?.length} > 1024 если приложена фотография. " +
                            "Измените текст или не удалите фотографию."
                },
            )
        } else {
            params.userActualizedInfo.apply {
                bcData = bcData?.copy(
                    text = params.update.message.text,
                )
            }
        }
        showBcConsole(params)
        params.userActualizedInfo.lastUserActionType = LastUserActionType.DEFAULT
    }

    private fun changePhoto(params: Params) {
        if (params.userActualizedInfo.bcData?.text?.length!! > 1024) {
            params.bot.execute(
                SendMessage().also {
                    it.text = "Ошибка! Невозможно добавить фотографию, длина текста " +
                        "${params.userActualizedInfo.bcData?.text?.length} > 1024. Измените текст или не " +
                        "прикладывайте фотографию"
                },
            )
        } else {
            params.userActualizedInfo.apply {
                bcData = bcData?.copy(
                    imageHash = params.update.message.photo.firstOrNull()?.fileId,
                )
            }
        }
        showBcConsole(params)
        params.userActualizedInfo.lastUserActionType = LastUserActionType.DEFAULT
    }

    private fun bcSendNow(params: Params) {
        params.userActualizedInfo.apply {
            bcData ?: return

            // удаляем консоль, она больше не нужна
            bcData?.lastConsoleMessageId?.let { consoleId ->
                params.bot.execute(
                    DeleteMessage().also {
                        it.chatId = params.update.message.chatId.toString()
                        it.messageId = consoleId
                    },
                )
            }

            broadcastRepository.save(
                bcData!!.copy(
                    lastConsoleMessageId = null,
                    isBuilt = true,
                    isScheduled = false,
                    isCompleted = false,
                    startTime = LocalDateTime.now().atZone(BOT_TIME_ZONE).toLocalDateTime(),
                ),
            )

            val okayMessage = "☃\uFE0F Что ж, я пошел делать рассылку! Как только закончу, обязательно сообщу тебе!"

            params.bot.execute(
                SendMessage().also {
                    it.chatId = params.update.message.chatId.toString()
                    it.text = okayMessage
                },
            )
        }
    }

    /**
     * Превью рассылки
     * ОСОБЕННОСТИ:
     *  - Пользователь только после предпросмотра сможет разослать / запланировать рассылку
     *  - Здесь используется нестандартная консоль (второе отправленное сообщение отмечается как console)
     */
    private fun bcPreview(params: Params) {
        removeBcConsole(params)
        // TODO: здесь отправка превью функцией Андрея
        // params.userActualizedInfo.data = id отправленного msg, чтобы потом удалять его?
        // вопрос: нужно ли удалять предпросмотр при дальнейшем редактированни / отмены / рассылки..

        val messageText = "<b>Конструктор рассылки</b>\n\nВыберите дальнейшее действие"
        val sendNow = CallbackData(callbackData = "#bc_send_now", metaText = "Разослать сейчас").save()
        val scheduleSending = CallbackData(
            callbackData = "#bc_to_schedule_console",
            metaText = "Запланировать рассылку",
        ).save()
        val backToBc = CallbackData(callbackData = "#bc_action_cancel", metaText = "Назад к конструктору").save()

        val keyboard =
            listOf(sendNow, scheduleSending, backToBc).map { button ->
                InlineKeyboardButton().also {
                    it.text = button.metaText!!
                    it.callbackData = button.id?.toString()
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

        params.userActualizedInfo.bcData = params.userActualizedInfo.bcData?.copy(
            lastConsoleMessageId = sent.messageId,
        )
    }

    private fun bcChangeTextMessage(params: Params) {
        removeBcConsole(params)
        val msgText = "*Напишите текст уведомления*\\.\n\nПравила оформления:\n" +
            "<b\\>текст</b\\> \\- жирный текст\n" +
            "<i\\>текст</i\\> \\- выделение курсивом\n" +
            "<u\\>текст</u\\> \\- подчеркнутый текст\n" +
            "<s\\>текст</s\\> \\- зачеркнутый текст\n" +
            "<code\\>текст</code\\> \\- выделенный текст \\(с копированием по клику\\)\n" +
            "<pre language\\=\"c\\+\\+\"\\>текст</pre\\> \\- исходный код или любой другой текст"

        val cancelButton = CallbackData(callbackData = "#bc_action_cancel", metaText = "Отмена").save()

        params.bot.execute(
            SendMessage().also {
                it.text = msgText
                it.chatId = params.userActualizedInfo.tui
                it.parseMode = ParseMode.MARKDOWNV2
                it.replyMarkup = createKeyboard(
                    listOf(
                        listOf(
                            InlineKeyboardButton().also {
                                it.text = cancelButton.metaText!!
                                it.callbackData = cancelButton.id?.toString()
                            },
                        ),
                    ),
                )
            },
        )
        params.userActualizedInfo.lastUserActionType = LastUserActionType.BC_TEXT_TYPE
    }

    private fun bcChangePhoto(params: Params) {
        removeBcConsole(params)
        val msgText = "Отправте фотографию, которую вы хотите прикрепить к рассылке"
        val cancelButton = CallbackData(callbackData = "#bc_action_cancel", metaText = "Отмена").save()

        params.bot.execute(
            SendMessage().also {
                it.text = msgText
                it.chatId = params.userActualizedInfo.tui
                it.parseMode = ParseMode.MARKDOWNV2
                it.replyMarkup = createKeyboard(
                    listOf(
                        listOf(
                            InlineKeyboardButton().also {
                                it.text = cancelButton.metaText!!
                                it.callbackData = cancelButton.id?.toString()
                            },
                        ),
                    ),
                )
            },
        )
        params.userActualizedInfo.lastUserActionType = LastUserActionType.BC_PHOTO_TYPE
    }

    private fun bcCancel(params: Params) {
        removeBcConsole(params)
        params.userActualizedInfo.bcData?.let {
            broadcastRepository.save(
                it.copy(
                    isDeleted = true,
                ),
            )
        }
        params.userActualizedInfo.bcData = null
    }

    private fun bcCancelAction(params: Params) {
        params.userActualizedInfo.lastUserActionType = LastUserActionType.DEFAULT
        showBcConsole(params)
    }

    private fun removeBcConsole(params: Params) {
        params.userActualizedInfo.apply {
            bcData ?: return
            bcData?.lastConsoleMessageId ?: return

            params.bot.execute(
                DeleteMessage().also {
                    it.chatId = tui
                    it.messageId = bcData?.lastConsoleMessageId!!
                },
            )
            bcData = bcData?.copy(
                lastConsoleMessageId = null,
            )
        }
    }

    private fun showBcConsole(params: Params, showPreview: Boolean = true) {
        val bcData = params.userActualizedInfo.bcData
        if (bcData == null || !showPreview) {
            params.userActualizedInfo.bcData = broadcastRepository.save(
                Broadcast(authorId = params.userActualizedInfo.id),
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
                        it.callbackData = button.id?.toString()
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

            params.userActualizedInfo.bcData = params.userActualizedInfo.bcData?.copy(
                lastConsoleMessageId = sent.messageId,
            )
        } else {
            val photoProp = CallbackData(
                callbackData = "#bc_change_photo",
                metaText = bcData.imageHash?.let { "Изменить фото" } ?: "Добавить фото",
            ).save()
            val textProp = CallbackData(
                callbackData = "#bc_change_text",
                metaText = bcData.text?.let { "Изменить текст" } ?: "Добавить текст",
            ).save()
            val addButton = CallbackData(callbackData = "#bc_add_button", metaText = "Добавить кнопку").save()

            val previewButton = CallbackData(callbackData = "#bc_preview", metaText = "Предпросмотр").save()
            val cancelButton = CallbackData(callbackData = "#bc_cancel", metaText = "Отмена").save()

            val keyboard = mutableListOf(
                photoProp,
                textProp,
                addButton,
                previewButton,
                cancelButton,
            ).apply {
                if (!params.update.message.hasText() && !params.update.message.hasPhoto()) {
                    remove(previewButton)
                }
                // TODO: если кол-во кнопок >=5 то здесь убрать кнопку 'добавление кнопки'
            }.map { callbackData ->
                listOf(
                    InlineKeyboardButton().also {
                        it.text = callbackData.metaText!!
                        it.callbackData = callbackData.id?.toString()
                    },
                )
            }

            val text = bcData.run {
                val title = "<b>Конструктор рассылки</b>\n\n"
                val text = text?.let { "Текст:\n${text}\n\n" } ?: ""
                val end = "Выберите дальнейшее действие"
                title + text + end
            }

            // TODO: кнопки

            removeBcConsole(params)

            // TODO: добавить везде где есть предпросмотр ? хз
            runCatching {
                when (bcData.imageHash) {
                    null -> params.bot.execute(
                        SendMessage().also {
                            it.chatId = params.userActualizedInfo.tui
                            it.text = text
                            it.replyMarkup = createKeyboard(keyboard)
                            it.parseMode = ParseMode.HTML
                        },
                    )

                    else -> params.bot.execute(
                        SendPhoto().also {
                            it.chatId = params.userActualizedInfo.tui
                            it.caption = text
                            it.replyMarkup = createKeyboard(keyboard)
                            it.photo = InputFile(bcData.imageHash)
                        },
                    )
                }
            }.onFailure {
                val failText = "\uD83D\uDE4A Ой! При отправке сообщения что-то пошло не так:\n" +
                    "<pre language=\"error\">${
                        it.message
                            ?.replace("<", "&lt;")
                            ?.replace(">", "&gt;")
                    }</pre>\n\nПопробуй еще раз."

                params.bot.execute(
                    SendMessage().also {
                        it.text = failText
                        it.chatId = params.userActualizedInfo.tui
                        it.parseMode = ParseMode.HTML
                    },
                )
            }.onSuccess {
                params.userActualizedInfo.bcData = params.userActualizedInfo.bcData?.copy(
                    lastConsoleMessageId = it.messageId,
                )
            }
        }
    }

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) =
        InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun CallbackData.save() = callbackDataRepository.save(this)

    private data class Params(
        val bot: Executor,
        val userActualizedInfo: UserActualizedInfo,
        val update: Update,
    )
}
