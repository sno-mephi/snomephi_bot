package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.annotation.FetcherPerms
import ru.idfedorov09.telegram.bot.base.util.UpdatesUtil
import ru.idfedorov09.telegram.bot.data.GlobalConstants.BOT_TIME_ZONE
import ru.idfedorov09.telegram.bot.data.GlobalConstants.MAX_BROADCAST_BUTTONS_COUNT
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.TextCommands.BROADCAST_CONSTRUCTOR
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.Broadcast
import ru.idfedorov09.telegram.bot.data.model.Button
import ru.idfedorov09.telegram.bot.data.model.ECallbackData
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.repo.ButtonRepository
import ru.idfedorov09.telegram.bot.repo.ECallbackDataRepository
import ru.idfedorov09.telegram.bot.repo.CategoryRepository
import ru.idfedorov09.telegram.bot.service.BroadcastSenderService
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.jvm.optionals.getOrNull

/**
 * Конструктор рассылки
 * bc - Broadcast Constructor
 */
@Component
class BroadcastConstructorFetcher(
    private val updatesUtil: UpdatesUtil,
    private val ECallbackDataRepository: ECallbackDataRepository,
    private val categoryRepository: CategoryRepository,
    private val broadcastRepository: BroadcastRepository,
    private val buttonRepository: ButtonRepository,
    private val broadcastSenderService: BroadcastSenderService,
    private val messageSenderService: MessageSenderService,
) : DefaultFetcher() {
    companion object {
        private val FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }

    @InjectData
    @FetcherPerms(UserRole.MAILER)
    fun doFetch(
        userActualizedInfo: UserActualizedInfo,
        update: Update,
    ) {
        val params =
            Params(
                userActualizedInfo,
                update,
                updatesUtil
            )
        when {
            update.hasMessage() && update.message.hasText() -> textCommandsHandler(params)
            update.hasCallbackQuery() -> callbackQueryHandler(params)
            update.hasMessage() && update.message.hasPhoto() -> photoHandler(params)
        }
    }

    private fun textCommandsHandler(params: Params) {
        val text = params.update.message.text

        text.apply {
            when {
                startsWith(BROADCAST_CONSTRUCTOR()) -> chooseType(params)
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
            LastUserActionType.BC_BUTTON_CAPTION_TYPE -> changeButtonCaption(params)
            LastUserActionType.BC_BUTTON_LINK_TYPE -> changeButtonLink(params)
            LastUserActionType.BC_BUTTON_CALLBACK_TYPING -> changeButtonCallback(params)
            LastUserActionType.BC_CHANGE_START_TIME -> changeStartTime(params)
            LastUserActionType.BC_CHANGE_CATEGORIES -> changeCategories(params)
            else -> return
        }
    }

    private fun callbackQueryHandler(params: Params) {
        val callbackId = params.update.callbackQuery.data?.toLongOrNull()
        callbackId ?: return
        val callbackData = ECallbackDataRepository.findById(callbackId).getOrNull() ?: return

        callbackData.callbackData?.apply {
            when {
                startsWith(CallbackCommands.BROADCAST_CANCEL.data) -> bcCancel(params)
                startsWith(CallbackCommands.BROADCAST_CHANGE_TEXT.data) -> bcChangeTextMessage(params)
                startsWith(CallbackCommands.BROADCAST_CHANGE_PHOTO.data) -> bcChangePhoto(params)
                startsWith(CallbackCommands.BROADCAST_TO_SCHEDULE_CONSOLE.data) -> bcChangeStartTime(params)
                startsWith(CallbackCommands.BROADCAST_DELETE_PHOTO.data) -> bcDeletePhoto(params)
                startsWith(CallbackCommands.BROADCAST_CHANGE_CATEGORIES.data) -> bcChangeCategories(params)
                startsWith(CallbackCommands.BROADCAST_ACTION_CANCEL.data) -> bcCancelAction(params)
                startsWith(CallbackCommands.BROADCAST_PREVIEW.data) -> bcPreview(params)
                startsWith(CallbackCommands.BROADCAST_SEND_NOW.data) -> bcSendNow(params)
                startsWith(CallbackCommands.BROADCAST_ADD_BUTTON.data) -> bcAddButton(params)
                startsWith(CallbackCommands.BROADCAST_CHANGE_BUTTON_CAPTION.data) -> changeButtonCaptionMessage(params)
                startsWith(CallbackCommands.BROADCAST_CHANGE_BUTTON_LINK.data) -> changeButtonLinkMessage(params)
                startsWith(CallbackCommands.BROADCAST_ACTION_SHOW_BTN_CONSOLE.data) -> showChangeButtonConsole(params)
                startsWith(CallbackCommands.BROADCAST_CHANGE_BUTTON_WITH_ID.data) -> editButton(params)
                startsWith(CallbackCommands.BROADCAST_BUTTON_REMOVE.data) -> removeButton(params)
                startsWith(CallbackCommands.BROADCAST_CHANGE_BUTTON_CALLBACK.data) -> changeButtonCallbackDataMessage(params)
                startsWith(CallbackCommands.BROADCAST_COMPLETE.data) -> bcComplete(params)
                startsWith(CallbackCommands.BROADCAST_START_COMMON.data) -> bcEntryPoint(params, false)
                startsWith(CallbackCommands.BROADCAST_START_WEEKLY.data) -> bcEntryPoint(params, true)
                startsWith(CallbackCommands.BROADCAST_WB_PREVIEW_STATE.data) -> bcToggleWebPreview(params)
            }
        }
    }

    private fun photoHandler(params: Params) {
        when (params.userActualizedInfo.lastUserActionType) {
            LastUserActionType.BC_PHOTO_TYPE -> changePhoto(params)
            else -> return
        }
    }

    private fun changeButtonCallback(params: Params) {
        val newCallbackDataText = params.update.message.text
        deleteUpdateMessage()

        params.userActualizedInfo.apply {
            id ?: return
            val button =
                buttonRepository.getLastModifiedButtonByUserId(id)?.copy(
                    callbackData = newCallbackDataText,
                    lastModifyTime = LocalDateTime.now(ZoneId.of("Europe/Moscow")),
                ) ?: return // TODO: если тут ретурн то чота сломалось
            buttonRepository.save(button)
            showChangeButtonConsole(params)
        }
    }

    private fun bcToggleWebPreview(params: Params) {
        params.userActualizedInfo.apply {
            bcData ?: return
            params.userActualizedInfo.bcData =
                broadcastRepository.save(
                    bcData!!.copy(
                        shouldShowWebPreview = !bcData!!.shouldShowWebPreview,
                    ),
                )
        }
        showBcConsole(params)
    }

    private fun changeButtonCallbackDataMessage(params: Params) {
        params.userActualizedInfo.apply {
            removeBcConsole(params)

            val backToBc =
                ECallbackData(
                    callbackData = CallbackCommands.BROADCAST_ACTION_SHOW_BTN_CONSOLE.data,
                    metaText = "К настройкам кнопки",
                ).save()

            // TODO: такую штуку в отдельный метод, много дублируется
            val keyboard =
                listOf(backToBc).map { button ->
                    InlineKeyboardButton().also {
                        it.text = button.metaText!!
                        it.callbackData = button.id?.toString()
                    }
                }.map { listOf(it) }

            val sentMessage =
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = tui,
                        replyMarkup = createKeyboard(keyboard),
                        text = "\uD83D\uDCDD Отправь мне текст коллбэка",
                    ),
                )
            bcData =
                bcData?.copy(
                    lastConsoleMessageId = sentMessage.messageId,
                )
            lastUserActionType = LastUserActionType.BC_BUTTON_CALLBACK_TYPING
        }
    }

    private fun changeText(params: Params) {
        params.userActualizedInfo.apply {
            if (bcData?.imageHash != null && params.update.message.text.length > 900) {
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = tui,
                        text =
                            "Ошибка! Невозможно добавить текст длины" +
                                " ${params.update.message.text.length} > 900 если приложена фотография. " +
                                "Измени текст или удали фотографию.",
                    ),
                )
            } else {
                bcData =
                    bcData?.copy(
                        text = params.update.message.text,
                    )
            }
            showBcConsole(params)
            lastUserActionType = LastUserActionType.DEFAULT
        }
    }

    private fun changePhoto(params: Params) {
        params.userActualizedInfo.apply {
            if ((bcData?.text?.length ?: 0) > 900) {
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = tui,
                        text =
                            "Ошибка! Невозможно добавить фотографию, длина текста " +
                                "${bcData?.text?.length} > 900. Измените текст или не " +
                                "прикладывайте фотографию",
                    ),
                )
            } else {
                val photoBroadcast = params.update.message.photo.last().fileId
                bcData =
                    bcData?.copy(
                        imageHash = photoBroadcast,
                    )
            }
            showBcConsole(params)
            lastUserActionType = LastUserActionType.DEFAULT

            deleteUpdateMessage()
        }
    }

    private fun changeStartTime(params: Params) {
        val msgText = params.update.message.text.trim()
        val startTime =
            when {
                msgText.matches(Regex("\\d{2}.\\d{2}.\\d{4} \\d{2}:\\d{2}")) -> resolveFullDate(msgText)
                msgText.matches(Regex("\\d{2}:\\d{2}")) -> resolveShortDate(msgText)
                else -> null
            } ?: run {
                bcChangeStartTime(params, prefix = "Неверный формат даты и времени")
                return
            }
        params.userActualizedInfo.apply {
            bcData =
                bcData?.copy(
                    startTime = startTime,
                )

            lastUserActionType = LastUserActionType.DEFAULT
        }
        bcChangeCategories(params)
    }

    /**
     * Возвращает по сообщению формата dd.MM.yyyy HH:mm текущую дату и время в LocalDateTime
     */
    private fun resolveFullDate(fullDateText: String) =
        LocalDateTime.parse(fullDateText, FORMATTER)
            .atZone(BOT_TIME_ZONE).toLocalDateTime()

    /**
     * Возвращает по сообщению формата HH:mm текущую дату с таким временем в LocalDateTime
     */
    private fun resolveShortDate(timeText: String): LocalDateTime {
        val nowDttm = LocalDateTime.now().atZone(BOT_TIME_ZONE).toLocalDateTime()
        val currentDate = nowDttm.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val formatString = "$currentDate $timeText"
        return LocalDateTime.parse(formatString, FORMATTER)
    }

    private fun changeCategories(params: Params) {
        params.userActualizedInfo.apply {
            val categorySuffix = params.update.message.text.substringAfter("/toggle_")
            val category = categoryRepository.findBySuffix(categorySuffix) ?: return
            if (bcData?.categoriesId?.contains(category.id) == true) {
                bcData?.categoriesId?.remove(category.id)
            } else {
                category.id?.let { bcData?.categoriesId?.add(it) }
            }
            bcChangeCategories(params)
        }
    }

    private fun bcSendNow(params: Params) {
        params.userActualizedInfo.apply {
            bcData ?: return

            // удаляем консоль, она больше не нужна
            bcData?.lastConsoleMessageId?.let { consoleId ->
                messageSenderService.deleteMessage(
                    MessageParams(
                        chatId = tui,
                        messageId = consoleId,
                    ),
                )
            }

            bcData =
                bcData?.copy(
                    lastConsoleMessageId = null,
                    isScheduled = false,
                    isCompleted = false,
                )
            bcChangeCategories(params)
        }
    }

    private fun bcComplete(params: Params) {
        params.userActualizedInfo.apply {
            bcData =
                bcData?.copy(
                    isBuilt = true,
                )
            bcData?.startTime ?: run {
                bcData =
                    bcData?.copy(
                        startTime = params.updatesUtil.getDate(params.update)
                            ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() },
                    )
            }
            val okayMessage = "☃\uFE0F Рассылка успешно создана! Обязательно сообщу, когда рассылка будет завершена!"

            messageSenderService.sendMessage(
                MessageParams(
                    chatId = tui,
                    text = okayMessage,
                ),
            )
            lastUserActionType = LastUserActionType.DEFAULT
        }
    }

    private fun removeButton(params: Params) {
        params.userActualizedInfo.apply {
            val button =
                buttonRepository.getLastModifiedButtonByUserId(id!!)?.copy(
                    text = null,
                ) ?: return
            buttonRepository.save(button)
            showBcConsole(params)
        }
    }

    private fun editButton(params: Params) {
        val callbackId = params.update.callbackQuery.data?.toLongOrNull()
        callbackId ?: return
        val callbackData = ECallbackDataRepository.findById(callbackId).getOrNull() ?: return
        val buttonId = callbackData.callbackData?.split("=")?.lastOrNull()?.toLongOrNull() ?: return

        val button = buttonRepository.findById(buttonId).getOrNull() ?: return
        buttonRepository.save(
            button.copy(
                lastModifyTime = LocalDateTime.now(ZoneId.of("Europe/Moscow")),
            ),
        )
        showChangeButtonConsole(params)
    }

    private fun bcAddButton(params: Params) {
        params.userActualizedInfo.apply {
            val buttons = buttonRepository.findAllValidButtonsForBroadcast(bcData?.id!!)
            if (buttons.size >= MAX_BROADCAST_BUTTONS_COUNT) {
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = tui,
                        text = "☠\uFE0F Ты добавил слишком много кнопок. Отредактируй или удали лишние плиз",
                    ),
                )
                return
            }

            buttonRepository.save(
                Button(
                    authorId = id,
                    broadcastId = bcData?.id,
                ),
            )

            changeButtonCaptionMessage(params, true)
        }
    }

    private fun changeButtonCaptionMessage(
        params: Params,
        backToDefaultConsole: Boolean = false,
    ) {
        params.userActualizedInfo.apply {
            val backToConsole =
                ECallbackData(
                    callbackData =
                        if (backToDefaultConsole) {
                            CallbackCommands.BROADCAST_ACTION_CANCEL.data
                        } else {
                            CallbackCommands.BROADCAST_ACTION_SHOW_BTN_CONSOLE.data
                        },
                    metaText = if (backToDefaultConsole) "Отменить создание кнопки" else "К настройкам кнопки",
                ).save()

            // TODO: такую штуку в отдельный метод, много дублируется
            val keyboard =
                listOf(backToConsole).map { button ->
                    InlineKeyboardButton().also {
                        it.text = button.metaText!!
                        it.callbackData = button.id?.toString()
                    }
                }.map { listOf(it) }

            removeBcConsole(params)
            val sentMessage =
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = tui,
                        text = "\uD83D\uDCDD Отправь мне текст, который будет отображаться на кнопке",
                        replyMarkup = createKeyboard(keyboard),
                    ),
                )
            bcData =
                bcData?.copy(
                    lastConsoleMessageId = sentMessage.messageId,
                )
            lastUserActionType = LastUserActionType.BC_BUTTON_CAPTION_TYPE
        }
    }

    private fun changeButtonLinkMessage(params: Params) {
        params.userActualizedInfo.apply {
            val backToBc =
                ECallbackData(
                    callbackData = CallbackCommands.BROADCAST_ACTION_SHOW_BTN_CONSOLE.data,
                    metaText = "К настройкам кнопки",
                ).save()

            // TODO: такую штуку в отдельный метод, много дублируется
            val keyboard =
                listOf(backToBc).map { button ->
                    InlineKeyboardButton().also {
                        it.text = button.metaText!!
                        it.callbackData = button.id?.toString()
                    }
                }.map { listOf(it) }

            removeBcConsole(params)
            val sentMessage =
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = tui,
                        text = "\uD83D\uDCDD Отправь мне текст с нужной ссылкой",
                        replyMarkup = createKeyboard(keyboard),
                    ),
                )
            bcData =
                bcData?.copy(
                    lastConsoleMessageId = sentMessage.messageId,
                )
            lastUserActionType = LastUserActionType.BC_BUTTON_LINK_TYPE
        }
    }

    private fun bcEntryPoint(
        params: Params,
        isWeekly: Boolean,
    ) {
        params.userActualizedInfo.apply {
            bcData =
                broadcastRepository.save(
                    Broadcast(
                        authorId = params.userActualizedInfo.id,
                        isWeekly = isWeekly,
                        lastConsoleMessageId = params.update.callbackQuery.message.messageId,
                        shouldShowWebPreview = false,
                    ),
                )
            showBcConsole(params)
        }
    }

    private fun changeButtonCaption(params: Params) {
        val caption = params.update.message.text
        deleteUpdateMessage()

        if (caption.length >= 32) {
            val backToBc =
                ECallbackData(
                    callbackData = CallbackCommands.BROADCAST_ACTION_SHOW_BTN_CONSOLE.data,
                    metaText = "К настройкам кнопки",
                ).save()

            // TODO: такую штуку в отдельный метод, много дублируется
            val keyboard =
                listOf(backToBc).map { button ->
                    InlineKeyboardButton().also {
                        it.text = button.metaText!!
                        it.callbackData = button.id?.toString()
                    }
                }.map { listOf(it) }

            messageSenderService.sendMessage(
                MessageParams(
                    chatId = params.userActualizedInfo.tui,
                    replyMarkup = createKeyboard(keyboard),
                    text =
                        "\uD83E\uDD21 Слишком длинная надпись для кнопки! " +
                            "Ограничение на длину символов: 32. Повтори попытку.\n\n" +
                            "\uD83D\uDCDD Отправь мне текст, который будет отображаться на кнопке",
                ),
            )
            return
        }

        params.userActualizedInfo.apply {
            id ?: return
            val button =
                buttonRepository.getLastModifiedButtonByUserId(id)?.copy(
                    text = caption,
                    lastModifyTime = LocalDateTime.now(ZoneId.of("Europe/Moscow")),
                ) ?: return // TODO: если тут ретурн то чота сломалось
            buttonRepository.save(button)
            showChangeButtonConsole(params)
        }
    }

    private fun changeButtonLink(params: Params) {
        val newUrl = params.update.message.text
        deleteUpdateMessage()

        params.userActualizedInfo.apply {
            id ?: return
            val button =
                buttonRepository.getLastModifiedButtonByUserId(id)?.copy(
                    link = newUrl,
                    lastModifyTime = LocalDateTime.now(ZoneId.of("Europe/Moscow")),
                ) ?: return // TODO: если тут ретурн то чота сломалось
            buttonRepository.save(button)
            showChangeButtonConsole(params)
        }
    }

    private fun showChangeButtonConsole(params: Params) {
        params.userActualizedInfo.apply {
            id ?: return
            val button =
                buttonRepository.getLastModifiedButtonByUserId(id)?.copy(
                    lastModifyTime = LocalDateTime.now(ZoneId.of("Europe/Moscow")),
                ) ?: return // TODO: если тут ретурн то чота сломалось

            removeBcConsole(params)

            val urlTextCode = button.link?.let { "<code>$it</code>" } ?: "пусто"
            val urlTextLink = button.link?.let { "(<a href='$it'>попробовать перейти</a>)" } ?: ""

            val caption = button.text?.let { "<code>$it</code>" } ?: "<b>текст не установлен!</b>"

            val callbackDataText = button.callbackData?.let { "<code>$it</code>" } ?: "<b>коллбэк не установлен</b>"

            val changeButtonCaption =
                ECallbackData(
                    callbackData = CallbackCommands.BROADCAST_CHANGE_BUTTON_CAPTION.data,
                    metaText = button.text?.let { "Изменить текст" } ?: "Добавить текст",
                ).save()

            val changeButtonLink =
                ECallbackData(
                    callbackData = CallbackCommands.BROADCAST_CHANGE_BUTTON_LINK.data,
                    metaText = button.link?.let { "Изменить ссылку" } ?: "Добавить ссылку",
                ).save()

            val changeButtonCallback =
                ECallbackData(
                    callbackData = CallbackCommands.BROADCAST_CHANGE_BUTTON_CALLBACK.data,
                    metaText = button.callbackData?.let { "Изменить коллбэк" } ?: "Добавить коллбэк",
                ).save()

            val removeButton =
                ECallbackData(
                    callbackData = CallbackCommands.BROADCAST_BUTTON_REMOVE.data,
                    metaText = "Удалить кнопку",
                ).save()

            val backToBc =
                ECallbackData(
                    callbackData = CallbackCommands.BROADCAST_ACTION_CANCEL.data,
                    metaText = "Назад к конструктору",
                ).save()

            // TODO: такую штуку в отдельный метод, много дублируется
            val keyboard =
                listOf(changeButtonCaption, changeButtonLink, changeButtonCallback, removeButton, backToBc)
                    .map { keyboardButton ->
                        InlineKeyboardButton().also {
                            it.text = keyboardButton.metaText!!
                            it.callbackData = keyboardButton.id?.toString()
                        }
                    }
                    .map { listOf(it) }

            val sentMessage =
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = tui,
                        text =
                            "Настройки кнопки:\n\n" +
                                "Надпись на кнопке: $caption\n" +
                                "Ссылка: $urlTextCode $urlTextLink\n" +
                                "Коллбэк: $callbackDataText",
                        parseMode = ParseMode.HTML,
                        replyMarkup = createKeyboard(keyboard),
                    ),
                )

            bcData =
                bcData?.copy(
                    lastConsoleMessageId = sentMessage.messageId,
                )
            lastUserActionType = LastUserActionType.DEFAULT
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

        // TODO: обработка ошибок
        broadcastSenderService.sendBroadcast(
            userId = params.userActualizedInfo.id!!,
            broadcast = params.userActualizedInfo.bcData!!,
            shouldAddToReceived = false,
        )

        val messageText = "<b>Конструктор рассылки</b>\n\nВыберите дальнейшее действие"
        val sendNow = ECallbackData(callbackData = CallbackCommands.BROADCAST_SEND_NOW.data, metaText = "Разослать сейчас").save()
        val scheduleSending =
            ECallbackData(
                callbackData = CallbackCommands.BROADCAST_TO_SCHEDULE_CONSOLE.data,
                metaText = "Запланировать рассылку",
            ).save()
        val backToBc = ECallbackData(callbackData = CallbackCommands.BROADCAST_ACTION_CANCEL.data, metaText = "Назад к конструктору").save()

        val keyboard =
            listOf(sendNow, scheduleSending, backToBc).map { button ->
                InlineKeyboardButton().also {
                    it.text = button.metaText!!
                    it.callbackData = button.id?.toString()
                }
            }.map { listOf(it) }

        val sent =
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = params.userActualizedInfo.tui,
                    text = messageText,
                    parseMode = ParseMode.HTML,
                    replyMarkup = createKeyboard(keyboard),
                ),
            )

        params.userActualizedInfo.bcData =
            params.userActualizedInfo.bcData?.copy(
                lastConsoleMessageId = sent.messageId,
            )
    }

    private fun bcChangeTextMessage(params: Params) {
        removeBcConsole(params)
        val msgText =
            "*Напишите текст уведомления*\\.\n\nПравила оформления:\n" +
                "<b\\>текст</b\\> \\- жирный текст\n" +
                "<i\\>текст</i\\> \\- выделение курсивом\n" +
                "<u\\>текст</u\\> \\- подчеркнутый текст\n" +
                "<s\\>текст</s\\> \\- зачеркнутый текст\n" +
                "<code\\>текст</code\\> \\- выделенный текст \\(с копированием по клику\\)\n" +
                "<pre language\\=\"c\\+\\+\"\\>текст</pre\\> \\- исходный код или любой другой текст\n" +
                "<a href\\='https://sno\\.mephi\\.ru/'\\>Сайт СНО</a\\> \\- ссылка"

        val cancelButton = ECallbackData(callbackData = CallbackCommands.BROADCAST_ACTION_CANCEL.data, metaText = "Отмена").save()

        val sent =
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = params.userActualizedInfo.tui,
                    text = msgText,
                    parseMode = ParseMode.MARKDOWNV2,
                    replyMarkup =
                        createKeyboard(
                            listOf(
                                listOf(
                                    InlineKeyboardButton().also {
                                        it.text = cancelButton.metaText!!
                                        it.callbackData = cancelButton.id?.toString()
                                    },
                                ),
                            ),
                        ),
                ),
            )
        params.userActualizedInfo.bcData =
            params.userActualizedInfo.bcData?.copy(
                lastConsoleMessageId = sent.messageId,
            )
        params.userActualizedInfo.lastUserActionType = LastUserActionType.BC_TEXT_TYPE
    }

    private fun bcChangePhoto(params: Params) {
        removeBcConsole(params)
        val msgText = "Отправьте фотографию, которую вы хотите прикрепить к рассылке"
        val cancelButton = ECallbackData(callbackData = CallbackCommands.BROADCAST_ACTION_CANCEL.data, metaText = "Отмена").save()
        val deletePhoto = ECallbackData(callbackData = CallbackCommands.BROADCAST_DELETE_PHOTO.data, metaText = "Удалить фото").save()
        val buttonsList =
            mutableListOf(
                listOf(
                    InlineKeyboardButton().also {
                        it.text = cancelButton.metaText!!
                        it.callbackData = cancelButton.id?.toString()
                    },
                ),
            )
        if (params.userActualizedInfo.bcData?.imageHash != null) {
            buttonsList.add(
                listOf(
                    InlineKeyboardButton().also {
                        it.text = deletePhoto.metaText!!
                        it.callbackData = deletePhoto.id?.toString()
                    },
                ),
            )
        }
        val sent =
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = params.userActualizedInfo.tui,
                    text = msgText,
                    replyMarkup = createKeyboard(buttonsList),
                ),
            )

        params.userActualizedInfo.bcData =
            params.userActualizedInfo.bcData?.copy(
                lastConsoleMessageId = sent.messageId,
            )
        params.userActualizedInfo.lastUserActionType = LastUserActionType.BC_PHOTO_TYPE
    }

    private fun bcChangeStartTime(
        params: Params,
        prefix: String? = null,
    ) {
        removeBcConsole(params)
        val msgStart = prefix?.let { "$prefix\n" } ?: ""
        val msgText =
            msgStart + "\uD83D\uDD57 Отправь время запуска рассылки в формате <b><i>ДД.ММ.ГГГГ ЧЧ:ММ</i></b>" +
                " или напиши время рассыли в формате <b><i>ЧЧ:ММ</i></b>, " +
                "если хочешь разослать <b><i><u>сегодня</u></i></b>\n\n" +
                "Например, если ты отправишь\n<pre>24.06.2077 19:25</pre>\nто рассылка начнется " +
                "24 июня 2077 года в 19:25, а если \n<pre>23:50</pre>\nто рассылка начнется <u>сегодня</u> в 23:50"
        val cancelButton = ECallbackData(callbackData = CallbackCommands.BROADCAST_ACTION_CANCEL.data, metaText = "Отмена").save()
        val sent =
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = params.userActualizedInfo.tui,
                    text = msgText,
                    replyMarkup =
                        createKeyboard(
                            listOf(
                                listOf(
                                    InlineKeyboardButton().also {
                                        it.text = cancelButton.metaText!!
                                        it.callbackData = cancelButton.id?.toString()
                                    },
                                ),
                            ),
                        ),
                    parseMode = ParseMode.HTML,
                ),
            )
        params.userActualizedInfo.bcData =
            params.userActualizedInfo.bcData?.copy(
                lastConsoleMessageId = sent.messageId,
            )
        params.userActualizedInfo.lastUserActionType = LastUserActionType.BC_CHANGE_START_TIME
    }

    private fun bcDeletePhoto(params: Params) {
        removeBcConsole(params)
        params.userActualizedInfo.apply {
            bcData =
                bcData?.copy(
                    imageHash = null,
                )
        }
        params.userActualizedInfo.lastUserActionType = LastUserActionType.DEFAULT
    }

    private fun bcChangeCategories(params: Params) {
        removeBcConsole(params)
        val backToBc = ECallbackData(callbackData = CallbackCommands.BROADCAST_ACTION_CANCEL.data, metaText = "Назад к конструктору").save()
        val cancelButton = ECallbackData(callbackData = CallbackCommands.BROADCAST_COMPLETE.data, metaText = "Подтвердить рассылку").save()
        params.userActualizedInfo.apply {
            val allCategoriesInfo =
                categoryRepository.findAll().map {
                    "<b>• ${it.title}\n</b>" +
                        "<i>${it.description?.let { "$it\n" }}</i>" +
                        if (bcData?.categoriesId?.contains(it.id) == true) {
                            "<b>Включено</b>"
                        } else {
                            "<b>Выключено</b>"
                        } + " - /toggle_${it.suffix}"
                }.joinToString(separator = "\n") { it }
            val msgText =
                "<b>Настройка категорий</b>\n\nВыберите категории рассылки (если все выключены, " +
                    "то рассылка будет по всем пользователям):\n\n$allCategoriesInfo"
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = tui,
                    text = msgText,
                    parseMode = ParseMode.HTML,
                    replyMarkup =
                        createKeyboard(
                            listOf(
                                listOf(
                                    InlineKeyboardButton().also {
                                        it.text = cancelButton.metaText!!
                                        it.callbackData = cancelButton.id?.toString()
                                    },
                                ),
                                listOf(
                                    InlineKeyboardButton().also {
                                        it.text = backToBc.metaText!!
                                        it.callbackData = backToBc.id?.toString()
                                    },
                                ),
                            ),
                        ),
                ),
            )
            lastUserActionType = LastUserActionType.BC_CHANGE_CATEGORIES
        }
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

            messageSenderService.deleteMessage(
                MessageParams(
                    chatId = tui,
                    messageId = bcData?.lastConsoleMessageId!!,
                ),
            )
            bcData =
                bcData?.copy(
                    lastConsoleMessageId = null,
                )
        }
    }

    private fun chooseType(params: Params) {
        val messageText = "Выберите тип рассылки"
        val common = ECallbackData(callbackData = CallbackCommands.BROADCAST_START_COMMON.data, metaText = "Обычная рассылка").save()
        val weeklyEvents = ECallbackData(callbackData = CallbackCommands.BROADCAST_START_WEEKLY.data, metaText = "Мероприятия недели").save()
        val keyboard =
            listOf(common, weeklyEvents).map { button ->
                InlineKeyboardButton().also {
                    it.text = button.metaText!!
                    it.callbackData = button.id?.toString()
                }
            }.map { listOf(it) }

        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                text = messageText,
                replyMarkup = createKeyboard(keyboard),
            ),
        )
    }

    private fun showBcConsole(
        params: Params,
        showPreview: Boolean = true,
    ) {
        params.userActualizedInfo.apply {
            if (bcData == null || !showPreview) {
                bcData ?: run {
                    bcData =
                        broadcastRepository.save(
                            Broadcast(authorId = params.userActualizedInfo.id),
                        )
                }
                val messageText = "<b>Конструктор рассылки</b>\n\nВыберите дальнейшее действие"
                val newPhoto = ECallbackData(callbackData = CallbackCommands.BROADCAST_CHANGE_PHOTO.data, metaText = "Добавить фото").save()
                val addText = ECallbackData(callbackData = CallbackCommands.BROADCAST_CHANGE_TEXT.data, metaText = "Добавить текст").save()
                val addButton = ECallbackData(callbackData = CallbackCommands.BROADCAST_ADD_BUTTON.data, metaText = "Добавить кнопку").save()
                val webPreviewButton = createWebPreviewToggleButton(bcData!!)
                val cancelButton = ECallbackData(callbackData = CallbackCommands.BROADCAST_CANCEL.data, metaText = "Отмена").save()

                val keyboard =
                    listOfNotNull(newPhoto, addText, addButton, webPreviewButton, cancelButton).map { button ->
                        InlineKeyboardButton().also {
                            it.text = button.metaText!!
                            it.callbackData = button.id?.toString()
                        }
                    }.map { listOf(it) }

                val sent =
                    messageSenderService.sendMessage(
                        MessageParams(
                            text = messageText,
                            parseMode = ParseMode.HTML,
                            replyMarkup = createKeyboard(keyboard),
                            chatId = params.userActualizedInfo.tui,
                            disableWebPagePreview = !params.userActualizedInfo.bcData!!.shouldShowWebPreview,
                        ),
                    )

                bcData =
                    bcData?.copy(
                        lastConsoleMessageId = sent.messageId,
                    )
            } else {
                removeBcConsole(params)
                bcData = params.userActualizedInfo.bcData

                val photoProp =
                    ECallbackData(
                        callbackData = CallbackCommands.BROADCAST_CHANGE_PHOTO.data,
                        metaText = bcData!!.imageHash?.let { "Изменить фото" } ?: "Добавить фото",
                    ).save()
                val textProp =
                    ECallbackData(
                        callbackData = CallbackCommands.BROADCAST_CHANGE_TEXT.data,
                        metaText = bcData!!.text?.let { "Изменить текст" } ?: "Добавить текст",
                    ).save()
                val addButton = ECallbackData(callbackData = CallbackCommands.BROADCAST_ADD_BUTTON.data, metaText = "Добавить кнопку").save()
                val webPreviewButton = createWebPreviewToggleButton(bcData!!)

                val previewButton = ECallbackData(callbackData = CallbackCommands.BROADCAST_PREVIEW.data, metaText = "Предпросмотр").save()
                val cancelButton = ECallbackData(callbackData = CallbackCommands.BROADCAST_CANCEL.data, metaText = "Отмена").save()

                val keyboardList =
                    listOfNotNull(
                        photoProp,
                        textProp,
                        addButton,
                        webPreviewButton,
                        previewButton,
                    ).toMutableList().apply {
                        addAll(
                            buttonRepository.findAllValidButtonsForBroadcast(bcData!!.id!!).map {
                                ECallbackData(
                                    callbackData = CallbackCommands.BROADCAST_CHANGE_BUTTON_WITH_ID.data + "=${it.id}",
                                    metaText = it.text,
                                ).save()
                            },
                        )
                    }
                keyboardList.add(cancelButton)

                val keyboard =
                    keyboardList.apply {
                        if (bcData!!.imageHash == null && bcData!!.text == null) {
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

                val text =
                    bcData?.run {
                        val title = "<b>Конструктор рассылки</b>\n\n"
                        val text = text?.let { "Текст:\n${text}\n\n" } ?: ""
                        val end = "Выберите дальнейшее действие"
                        title + text + end
                    } ?: "Error!!!"

                // TODO: добавить везде где есть предпросмотр ? хз
                runCatching {
                    when (bcData?.imageHash) {
                        null ->
                            messageSenderService.sendMessage(
                                MessageParams(
                                    chatId = params.userActualizedInfo.tui,
                                    text = text,
                                    replyMarkup = createKeyboard(keyboard),
                                    parseMode = ParseMode.HTML,
                                    disableWebPagePreview = !params.userActualizedInfo.bcData!!.shouldShowWebPreview,
                                ),
                            )

                        else ->
                            messageSenderService.sendMessage(
                                MessageParams(
                                    chatId = params.userActualizedInfo.tui,
                                    text = text,
                                    parseMode = ParseMode.HTML,
                                    replyMarkup = createKeyboard(keyboard),
                                    photo = InputFile(bcData?.imageHash),
                                    disableWebPagePreview = !params.userActualizedInfo.bcData!!.shouldShowWebPreview,
                                ),
                            )
                    }
                }.onFailure {
                    val failText =
                        "\uD83D\uDE4A Ой! При отправке сообщения что-то пошло не так:\n" +
                            "<pre language=\"error\">${
                                it.message
                                    ?.replace("<", "&lt;")
                                    ?.replace(">", "&gt;")
                            }</pre>\n\nПопробуй еще раз."

                    messageSenderService.sendMessage(
                        MessageParams(
                            text = failText,
                            chatId = params.userActualizedInfo.tui,
                            parseMode = ParseMode.HTML,
                        ),
                    )
                }.onSuccess {
                    bcData =
                        bcData?.copy(
                            lastConsoleMessageId = it.messageId,
                        )
                }
            }
            params.userActualizedInfo.lastUserActionType = LastUserActionType.DEFAULT
        }
    }

    private fun createWebPreviewToggleButton(broadcast: Broadcast): ECallbackData? {
        if (broadcast.isWeekly) return null
        val smile = if (broadcast.shouldShowWebPreview) "✅" else "❌"
        val state = if (broadcast.shouldShowWebPreview) "(вкл)" else "(выкл)"
        val text = "$smile Превью веб-страницы $state"
        return ECallbackData(callbackData = CallbackCommands.BROADCAST_WB_PREVIEW_STATE.data, metaText = text).save()
    }

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun ECallbackData.save() = ECallbackDataRepository.save(this)

    private data class Params(
        var userActualizedInfo: UserActualizedInfo,
        val update: Update,
        val updatesUtil: UpdatesUtil,
    )
}
