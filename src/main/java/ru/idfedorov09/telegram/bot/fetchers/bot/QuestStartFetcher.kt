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
import ru.idfedorov09.telegram.bot.data.model.*
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.QuestMessageRepository
import ru.idfedorov09.telegram.bot.repo.QuestDialogRepository
import ru.idfedorov09.telegram.bot.repo.QuestSegmentRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.idfedorov09.telegram.bot.util.MessageSenderUtil
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import java.time.Instant
import java.time.ZoneId

@Component
class QuestStartFetcher(
    private val updatesUtil: UpdatesUtil,
    private val questDialogRepository: QuestDialogRepository,
    private val questSegmentRepository: QuestSegmentRepository,
    private val questMessageRepository: QuestMessageRepository,
    private val messageSenderService: MessageSenderService,
) : DefaultFetcher() {
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
            if (activeQuestDialog != null ||
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

        val questSegment = QuestSegment(
            startTime = updatesUtil.getDate(update)
                ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() }
        ).let { questSegmentRepository.save(it) }

        val questDialog =
            QuestDialog(
                authorId = userActualizedInfo.id,
                questionStatus = QuestionStatus.WAIT,
                startTime = updatesUtil.getDate(update)
                    ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() },
                lastQuestSegmentId = questSegment.id
            ).let { questDialogRepository.save(it) }

        questSegmentRepository.save(
            questSegment.copy(
                questId = questDialog.id
            )
        )

        val questMessage =
            QuestMessage(
                questId = questDialog.id,
                segmentId = questSegment.id,
                isByQuestionAuthor = true,
                authorId = userActualizedInfo.id,
                messageText = messageText,
                messageId = update.message.messageId,
                messageDocumentHash = documentHash,
                messagePhotoHash = photoHash,
                messageTime = updatesUtil.getDate(update)
                    ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() }
            ).let { questMessageRepository.save(it) }

        questDialog.dialogHistory.add(questMessage.id!!)

        messageSenderService.sendMessage(
            MessageParams(
                chatId = userActualizedInfo.tui,
                text = "✉\uFE0F Сформировано обращение #${questDialog.id}. Ожидайте ответа.",
            ),
        )

        // TODO: добавить время обращения
        messageSenderService.sendMessage(
            MessageParams(
                chatId = QUEST_RESPONDENT_CHAT_ID,
                text =
                    "\uD83D\uDCE5 Получен вопрос #${questDialog.id} " +
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
                    replyMarkup = createChooseKeyboard(questDialog),
                ),
            )

        questDialog.copy(
            consoleMessageId = sentMessage.messageId.toString(),
        ).also { questDialogRepository.save(it) }
    }

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun createChooseKeyboard(questDialog: QuestDialog) =
        createKeyboard(
            listOf(
                listOf(
                    InlineKeyboardButton("\uD83D\uDCAC Ответ")
                        .also { it.callbackData = QUEST_ANSWER.format(questDialog.id) },
                ),
                listOf(
                    InlineKeyboardButton("\uD83D\uDD07 Игнор")
                        .also { it.callbackData = QUEST_IGNORE.format(questDialog.id) },
                    // TODO: убираем кнопку бана до тех пор пока не проработаем систему банов до конца
//                    InlineKeyboardButton("\uD83D\uDEAF Бан")
//                        .also { it.callbackData = QUEST_BAN.format(questDialog.id) },
                ),
            ),
        )
}
