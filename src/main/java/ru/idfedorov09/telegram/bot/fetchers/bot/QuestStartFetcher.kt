package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.base.util.UpdatesUtil
import ru.idfedorov09.telegram.bot.data.GlobalConstants.QUEST_RESPONDENT_CHAT_ID
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.*
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.*
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.fetchers.AddCertificateFetcher.Companion.hasCertificate
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.idfedorov09.telegram.bot.repo.QuestDialogRepository
import ru.idfedorov09.telegram.bot.repo.QuestMessageRepository
import ru.idfedorov09.telegram.bot.repo.QuestSegmentRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.idfedorov09.telegram.bot.util.MessageSenderUtil
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
    private val callbackDataRepository: CallbackDataRepository,
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
                lastUserActionType != LastUserActionType.DEFAULT
            ) {
                return
            }
            // если апдейт из беседы, то игнорим
            if (update.message.chatId.toString() != tui) return

            // если отправил сообщение мэилер и тип полученного документа - pdf, то ничего не делаем;
            // вероятно, отправили сертификат
            if (update.hasCertificate(roles)) {
                return
            }
        }

        ask(update, userActualizedInfo)
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

        val questSegment =
            QuestSegment(
                startTime =
                    updatesUtil.getDate(update)
                        ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() },
            ).let { questSegmentRepository.save(it) }

        val questDialog =
            QuestDialog(
                authorId = userActualizedInfo.id,
                questionStatus = QuestionStatus.WAIT,
                startTime =
                    updatesUtil.getDate(update)
                        ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() },
                lastQuestSegmentId = questSegment.id,
            ).let { questDialogRepository.save(it) }

        questSegmentRepository.save(
            questSegment.copy(
                questId = questDialog.id,
            ),
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
                messageTime =
                    updatesUtil.getDate(update)
                        ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() },
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

        val answerButton =
            CallbackData(
                callbackData = QUEST_ANSWER.format(questDialog.id),
                metaText = "\uD83D\uDCAC Ответ",
            ).save()
        val banButton =
            CallbackData(
                callbackData = QUEST_IGNORE.format(questDialog.id),
                metaText = "\uD83D\uDD07 Игнор",
            ).save()
        val ignoreButton =
            CallbackData(
                callbackData = BANNED_USER.format(questDialog.id, userActualizedInfo.tui.toLong()),
                metaText = "\uD83D\uDEAF Бан",
            ).save()

        val sentMessage =
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = QUEST_RESPONDENT_CHAT_ID,
                    text = "Выберите действие:",
                    replyMarkup = createChooseKeyboard(answerButton, banButton, ignoreButton),
                ),
            )

        questDialog.copy(
            consoleMessageId = sentMessage.messageId.toString(),
        ).also { questDialogRepository.save(it) }
    }

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun createChooseKeyboard(vararg callbackData: CallbackData): InlineKeyboardMarkup {
        val (first, rest) = callbackData.withIndex().partition { it.index == 0 }
        val firstList =
            first.map { it.value }.map { button ->
                InlineKeyboardButton().also {
                    it.text = button.metaText!!
                    it.callbackData = button.id?.toString()
                }
            }
        val secondList =
            rest.map { it.value }.map { button ->
                InlineKeyboardButton().also {
                    it.text = button.metaText!!
                    it.callbackData = button.id?.toString()
                }
            }
        return createKeyboard(listOf(firstList, secondList))
    }

    private fun CallbackData.save() = callbackDataRepository.save(this)
}
