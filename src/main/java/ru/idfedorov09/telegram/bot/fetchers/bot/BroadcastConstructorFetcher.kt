package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.GlobalConstants.BOT_TIME_ZONE
import ru.idfedorov09.telegram.bot.data.GlobalConstants.MAX_BROADCAST_BUTTONS_COUNT
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.TextCommands.BROADCAST_CONSTRUCTOR
import ru.idfedorov09.telegram.bot.data.model.Broadcast
import ru.idfedorov09.telegram.bot.data.model.Button
import ru.idfedorov09.telegram.bot.data.model.CallbackData
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.repo.ButtonRepository
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.idfedorov09.telegram.bot.repo.CategoryRepository
import ru.idfedorov09.telegram.bot.service.BroadcastSenderService
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher
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
    private val callbackDataRepository: CallbackDataRepository,
    private val categoryRepository: CategoryRepository,
    private val broadcastRepository: BroadcastRepository,
    private val buttonRepository: ButtonRepository,
    private val broadcastSenderService: BroadcastSenderService,
    private val messageSenderService: MessageSenderService,
) : DefaultFetcher() {
    @InjectData
    fun doFetch(
        userActualizedInfo: UserActualizedInfo,
        update: Update,
    ) {
        val params =
            Params(
                userActualizedInfo,
                update,
            )
        when {
            update.hasMessage() && update.message.hasText() -> textCommandsHandler(params)
            update.hasCallbackQuery() -> callbackQueryHandler(update, params)
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

    private fun callbackQueryHandler(
        update: Update,
        params: Params,
    ) {
        val callbackId = update.callbackQuery.data?.toLongOrNull()
        callbackId ?: return
        val callbackData = callbackDataRepository.findById(callbackId).getOrNull() ?: return

        callbackData.callbackData?.apply {
            when {
                startsWith("#bc_cancel") -> bcCancel(params)
                startsWith("#bc_change_text") -> bcChangeTextMessage(params)
                startsWith("#bc_change_photo") -> bcChangePhoto(params)
                startsWith("#bc_to_schedule_console") -> bcChangeStartTime(params)
                startsWith("#bc_delete_photo") -> bcDeletePhoto(params)
                startsWith("#bc_change_categories") -> bcChangeCategories(params)
                startsWith("#bc_action_cancel") -> bcCancelAction(params)
                startsWith("#bc_preview") -> bcPreview(params)
                startsWith("#bc_send_now") -> bcSendNow(params)
                startsWith("#bc_add_button") -> bcAddButton(params)
                startsWith("#bc_change_button_caption") -> changeButtonCaptionMessage(params)
                startsWith("#bc_change_button_link") -> changeButtonLinkMessage(params)
                startsWith("#bc_action_show_btn_console") -> showChangeButtonConsole(params)
                startsWith("#bc_change_button_with_id") -> editButton(params)
                startsWith("#bc_button_remove") -> removeButton(params)
                startsWith("#bc_change_button_callback") -> changeButtonCallbackDataMessage(params)
                startsWith("#bc_complete") -> bcComplete(params)
                startsWith("#bc_start_common") -> bcEntryPoint(params, false)
                startsWith("#bc_start_weekly") -> bcEntryPoint(params, true)
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

    private fun changeButtonCallbackDataMessage(params: Params) {
        params.userActualizedInfo.apply {
            removeBcConsole(params)

            val backToBc =
                CallbackData(
                    callbackData = "#bc_action_show_btn_console",
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
        }
    }

    private fun changeStartTime(params: Params) {
        val msgText = params.update.message.text
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        if (!msgText.matches(Regex("\\d{2}.\\d{2}.\\d{4} \\d{2}:\\d{2}"))) {
            bcChangeStartTime(params, prefix = "Неверный формат даты и времени")
            return
        }
        val startTime = LocalDateTime.parse(msgText, formatter)
        params.userActualizedInfo.apply {
            bcData =
                bcData?.copy(
                    startTime = startTime,
                )
        }

        params.userActualizedInfo.lastUserActionType = LastUserActionType.DEFAULT
        bcChangeCategories(params)
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
                        startTime = LocalDateTime.now().atZone(BOT_TIME_ZONE).toLocalDateTime(),
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
        val callbackData = callbackDataRepository.findById(callbackId).getOrNull() ?: return
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
                CallbackData(
                    callbackData = if (backToDefaultConsole) "#bc_action_cancel" else "#bc_action_show_btn_console",
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
                CallbackData(
                    callbackData = "#bc_action_show_btn_console",
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
                    ),
                )
            showBcConsole(params)
        }
    }

    private fun changeButtonCaption(params: Params) {
        val caption = params.update.message.text
        if (caption.length >= 32) {
            val backToBc =
                CallbackData(
                    callbackData = "#bc_action_show_btn_console",
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
                CallbackData(
                    callbackData = "#bc_change_button_caption",
                    metaText = button.text?.let { "Изменить текст" } ?: "Добавить текст",
                ).save()

            val changeButtonLink =
                CallbackData(
                    callbackData = "#bc_change_button_link",
                    metaText = button.link?.let { "Изменить ссылку" } ?: "Добавить ссылку",
                ).save()

            val changeButtonCallback =
                CallbackData(
                    callbackData = "#bc_change_button_callback",
                    metaText = button.callbackData?.let { "Изменить коллбэк" } ?: "Добавить коллбэк",
                ).save()

            val removeButton =
                CallbackData(
                    callbackData = "#bc_button_remove",
                    metaText = "Удалить кнопку",
                ).save()

            val backToBc =
                CallbackData(
                    callbackData = "#bc_action_cancel",
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
        val sendNow = CallbackData(callbackData = "#bc_send_now", metaText = "Разослать сейчас").save()
        val scheduleSending =
            CallbackData(
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

        val cancelButton = CallbackData(callbackData = "#bc_action_cancel", metaText = "Отмена").save()

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
        val cancelButton = CallbackData(callbackData = "#bc_action_cancel", metaText = "Отмена").save()
        val deletePhoto = CallbackData(callbackData = "#bc_delete_photo", metaText = "Удалить фото").save()

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
                            listOf(
                                InlineKeyboardButton().also {
                                    it.text = deletePhoto.metaText!!
                                    it.callbackData = deletePhoto.id?.toString()
                                },
                            ),
                        ),
                    ),
            ),
        )
        params.userActualizedInfo.lastUserActionType = LastUserActionType.BC_PHOTO_TYPE
    }

    private fun bcChangeStartTime(
        params: Params,
        prefix: String? = null,
    ) {
        removeBcConsole(params)
        val msgStart = prefix?.let { "$prefix\n" } ?: ""
        val msgText = msgStart + "Отправьте время запуска рассылки в формате 'дд.мм.гггг чч:мм'"
        val cancelButton = CallbackData(callbackData = "#bc_action_cancel", metaText = "Отмена").save()
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
        val backToBc = CallbackData(callbackData = "#bc_action_cancel", metaText = "Назад к конструктору").save()
        val cancelButton = CallbackData(callbackData = "#bc_complete", metaText = "Подтвердить рассылку").save()
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
        val common = CallbackData(callbackData = "#bc_start_common", metaText = "Обычная рассылка").save()
        val weeklyEvents = CallbackData(callbackData = "#bc_start_weekly", metaText = "Мероприятия недели").save()
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

                val sent =
                    messageSenderService.sendMessage(
                        MessageParams(
                            text = messageText,
                            parseMode = ParseMode.HTML,
                            replyMarkup = createKeyboard(keyboard),
                            chatId = params.userActualizedInfo.tui,
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
                    CallbackData(
                        callbackData = "#bc_change_photo",
                        metaText = bcData!!.imageHash?.let { "Изменить фото" } ?: "Добавить фото",
                    ).save()
                val textProp =
                    CallbackData(
                        callbackData = "#bc_change_text",
                        metaText = bcData!!.text?.let { "Изменить текст" } ?: "Добавить текст",
                    ).save()
                val addButton = CallbackData(callbackData = "#bc_add_button", metaText = "Добавить кнопку").save()

                val previewButton = CallbackData(callbackData = "#bc_preview", metaText = "Предпросмотр").save()
                val cancelButton = CallbackData(callbackData = "#bc_cancel", metaText = "Отмена").save()

                val keyboardList =
                    mutableListOf(
                        photoProp,
                        textProp,
                        addButton,
                        previewButton,
                    ).apply {
                        addAll(
                            buttonRepository.findAllValidButtonsForBroadcast(bcData!!.id!!).map {
                                CallbackData(
                                    callbackData = "#bc_change_button_with_id=${it.id}",
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

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun CallbackData.save() = callbackDataRepository.save(this)

    private data class Params(
        var userActualizedInfo: UserActualizedInfo,
        val update: Update,
    )
}
