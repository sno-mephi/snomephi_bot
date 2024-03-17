package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.GlobalConstants.QUEST_RESPONDENT_CHAT_ID
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.*
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.Quest
import ru.idfedorov09.telegram.bot.data.model.QuestDialogMessage
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.repo.QuestDialogMessageRepository
import ru.idfedorov09.telegram.bot.repo.QuestRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.idfedorov09.telegram.bot.util.MessageSenderUtil
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

@Component
class QuestStartFetcher(
    private val updatesUtil: UpdatesUtil,
    private val questRepository: QuestRepository,
    private val questDialogMessageRepository: QuestDialogMessageRepository,
    private val messageSenderService: MessageSenderService,
) : GeneralFetcher() {
    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ) {
        update.apply {
            if (!(hasMessage() && (message.hasText() || message.hasDocument() || message.hasPhoto()))) return
        }

        userActualizedInfo.apply {
            // создаем новый вопрос если пользователь сейчас не в активном диалоге
            if (activeQuest != null ||
                !(
                    lastUserActionType == LastUserActionType.DEFAULT ||
                        lastUserActionType == LastUserActionType.ACT_QUEST_ANS_CLICK
                )
            ) {
                return
            }
            // если апдейт из беседы, то игнорим
            if (update.message.chatId.toString() != tui) return
        }

        when {
            // если была нажата кнопка на ожидание ответа, то значит следующим сообщением будет отправлен ответ
            userActualizedInfo.lastUserActionType == LastUserActionType.ACT_QUEST_ANS_CLICK ->
                giveAnswer(update, userActualizedInfo)

            else -> ask(update, userActualizedInfo)
        }
    }

    /**
     * Метод обрабатывающий апдейт на выдачу ответа одним сообщением
     */
    private fun giveAnswer(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ) {
        // TODO: собрать таблицу Actions и по ней определять, на какой вопрос собирается ответить челик
    }

    /**
     * Метод, обрабатывающий апдейт на задавание вопроса
     */
    private fun ask(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ) {
        val messageText = update.message.text ?: update.message.caption

        val photoHash =
            if (update.message.hasPhoto()) {
                update.message.photo.last().fileId
            } else {
                null
            }

        val documentHash =
            if (update.message.hasDocument()) {
                update.message.document.fileId
            } else {
                null
            }

        // если пришла команда - ничего не делаем
        if (TextCommands.isTextCommand(messageText)) return

        val quest =
            Quest(
                authorId = userActualizedInfo.id,
                questionStatus = QuestionStatus.WAIT,
            ).let { questRepository.save(it) }

        val questDialogMessage =
            QuestDialogMessage(
                questId = quest.id,
                isByQuestionAuthor = true,
                authorId = userActualizedInfo.id,
                messageText = messageText,
                messageId = update.message.messageId,
                messageDocumentHash = documentHash,
                messagePhotoHash = photoHash,
            ).let { questDialogMessageRepository.save(it) }

        quest.dialogHistory.add(questDialogMessage.id!!)

        messageSenderService.sendMessage(
            MessageParams(
                chatId = userActualizedInfo.tui,
                text = "✉\uFE0F Сформировано обращение #${quest.id}. Ожидайте ответа.",
            ),
        )

        // TODO: добавить время обращения
        messageSenderService.sendMessage(
            MessageParams(
                chatId = QUEST_RESPONDENT_CHAT_ID,
                text =
                    "\uD83D\uDCE5 Получен вопрос #${quest.id} " +
                        "от ${MessageSenderUtil.userName(userActualizedInfo.lastTgNick, userActualizedInfo.fullName)}",
            ),
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = QUEST_RESPONDENT_CHAT_ID,
                fromChatId = updatesUtil.getChatId(update).toString(),
                messageId = update.message.messageId,
            ),
        )

        val sentMessage =
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = QUEST_RESPONDENT_CHAT_ID,
                    text = "Выберите действие:",
                    replyMarkup = createChooseKeyboard(quest),
                ),
            )

        quest.copy(
            consoleMessageId = sentMessage.messageId.toString(),
        ).also { questRepository.save(it) }
    }

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun createChooseKeyboard(quest: Quest) =
        createKeyboard(
            listOf(
                listOf(
                    InlineKeyboardButton("\uD83D\uDCAC Ответ")
                        .also { it.callbackData = QUEST_ANSWER.format(quest.id) },
                ),
                listOf(
                    InlineKeyboardButton("\uD83D\uDD07 Игнор")
                        .also { it.callbackData = QUEST_IGNORE.format(quest.id) },
                    InlineKeyboardButton("\uD83D\uDEAF Бан")
                        .also { it.callbackData = QUEST_BAN.format(quest.id) },
                ),
            ),
        )
}
