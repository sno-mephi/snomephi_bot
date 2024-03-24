package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.GlobalConstants.QUEST_RESPONDENT_CHAT_ID
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_ANSWER
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_BAN
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_IGNORE
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_START_DIALOG
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus
import ru.idfedorov09.telegram.bot.data.enums.UserKeyboardType
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.QuestDialog
import ru.idfedorov09.telegram.bot.data.model.QuestSegment
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.QuestMessageRepository
import ru.idfedorov09.telegram.bot.repo.QuestDialogRepository
import ru.idfedorov09.telegram.bot.repo.QuestSegmentRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.idfedorov09.telegram.bot.service.SwitchKeyboardService
import ru.idfedorov09.telegram.bot.util.MessageSenderUtil
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import java.lang.NumberFormatException
import java.time.Instant
import java.time.ZoneId
import kotlin.jvm.optionals.getOrNull
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
/**
 * Фетчер, обрабатывающий случаи нажатия на кнопки для вопросов
 */
@Component
class QuestButtonHandlerFetcher(
    private val updatesUtil: UpdatesUtil,
    private val bot: Executor,
    private val messageSenderService: MessageSenderService,
    private val questDialogRepository: QuestDialogRepository,
    private val questSegmentRepository: QuestSegmentRepository,
    private val userRepository: UserRepository,
    private val questMessageRepository: QuestMessageRepository,
    private val switchKeyboardService: SwitchKeyboardService,
) : DefaultFetcher() {
    // TODO: обработать случай когда бот не может написать пользователю!
    // TODO: нельзя отвечать самому себе
    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ): UserActualizedInfo {
        if (!update.hasCallbackQuery()) return userActualizedInfo
        val callbackData = update.callbackQuery.data
        if (!Regex("^.*\\|\\d+$").matches(callbackData) ||
            !userActualizedInfo.isRegistered
        ) {
            return userActualizedInfo
        }

        val questByCallbackData = getQuestByCallbackData(callbackData) ?: return userActualizedInfo
        val segment = questByCallbackData.lastQuestSegmentId?.let { questSegmentRepository.findById(it).get() }

        val requestData =
            RequestData(
                questByCallbackData,
                segment!!,
                userActualizedInfo,
                update,
            )
        return when {
                QUEST_ANSWER.isMatch(callbackData) -> clickAnswer(requestData)
                QUEST_IGNORE.isMatch(callbackData) -> clickIgnore(requestData)
                QUEST_BAN.isMatch(callbackData) -> clickBan(requestData)
                QUEST_START_DIALOG.isMatch(callbackData) -> clickStartDialog(requestData)
                else -> userActualizedInfo
            }
        }

    private fun clickStartDialog(data: RequestData): UserActualizedInfo {
        if (data.questDialog.questionStatus != QuestionStatus.WAIT){
            val callbackAnswer = AnswerCallbackQuery().also{
                it.text = "\uD83D\uDC40 Возможно, на этот вопрос уже ответили или отвечают"
                it.callbackQueryId = data.update.callbackQuery.id
            }
            bot.execute(callbackAnswer)
            return data.userActualizedInfo
        }
        if (data.userActualizedInfo.activeQuestDialog != null) return data.userActualizedInfo

        val quest =
            data.questDialog.copy(
                questionStatus = QuestionStatus.DIALOG,
            )

        questSegmentRepository.save(
            data.questSegment.copy(
                responderId = data.userActualizedInfo.id,
            )
        )

        val questionAuthor = userRepository.findActiveUsersById(data.questDialog.authorId!!)!!
        questDialogRepository.save(quest)
        userRepository.save(
            questionAuthor.copy(
                questDialogId = data.questDialog.id,
            ),
        )

        switchKeyboardService.switchKeyboard(
            questionAuthor.id!!,
            UserKeyboardType.DIALOG_QUEST,
        )
        switchKeyboardService.switchKeyboard(
            data.userActualizedInfo.id!!,
            UserKeyboardType.DIALOG_QUEST,
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = questionAuthor.tui!!,
                text = "<i>С вами общается оператор по поводу обращения #${data.questDialog.id}</i>",
                parseMode = ParseMode.HTML,
            ),
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = data.userActualizedInfo.tui,
                text =
                    "<i>Ты перешел в диалог с пользователем ${MessageSenderUtil.userName(
                        questionAuthor.lastTgNick,
                        questionAuthor.fullName,
                    )}. " +
                        "Несмотря на твою анонимность, оставайся вежливым :)</i>",
                parseMode = ParseMode.HTML,
            ),
        )

        messageSenderService.editMessage(
            MessageParams(
                chatId = QUEST_RESPONDENT_CHAT_ID,
                messageId = quest.consoleMessageId!!.toInt(),
                text =
                    "✏\uFE0F ${MessageSenderUtil.userName(data.userActualizedInfo.lastTgNick, data.userActualizedInfo.fullName)} " +
                        "ведет диалог",
            ),
        )

        return data.userActualizedInfo.copy(
            activeQuestDialog = data.questDialog,
        )
    }

    private fun clickIgnore(data: RequestData): UserActualizedInfo {
        if (data.questDialog.questionStatus != QuestionStatus.WAIT) return data.userActualizedInfo

        questDialogRepository.save(
            data.questDialog.copy(
                questionStatus = QuestionStatus.CLOSED,
                finishTime = updatesUtil.getDate(data.update)
                    ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() }
            ),
        )

        questSegmentRepository.save(
            data.questSegment.copy(
                responderId = data.userActualizedInfo.id,
                finishTime = updatesUtil.getDate(data.update)
                    ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() }
            )
        )

        val newText = "\uD83D\uDFE1 Проигнорировано пользователем ${MessageSenderUtil.userName(
            data.userActualizedInfo.lastTgNick,
            data.userActualizedInfo.fullName,
        )}."
        messageSenderService.editMessage(
            MessageParams(
                chatId = QUEST_RESPONDENT_CHAT_ID,
                messageId = data.questDialog.consoleMessageId?.toInt(),
                text = newText,
            ),
        )
        return data.userActualizedInfo
    }

    private fun clickAnswer(data: RequestData): UserActualizedInfo {
        if (data.questDialog.questionStatus != QuestionStatus.WAIT) return data.userActualizedInfo

        val questionAuthor = userRepository.findActiveUsersById(data.questDialog.authorId!!)!!

        if (data.userActualizedInfo.tui == questionAuthor.tui){

            val answerCallbackQuery = AnswerCallbackQuery().also {
                it.callbackQueryId = data.update.callbackQuery.id
                it.text = "Вы не можете отвечать самому себе!"
                it.showAlert = true
            }
            bot.execute(answerCallbackQuery)

            return data.userActualizedInfo
        }

        if (data.userActualizedInfo.activeQuestDialog != null) return data.userActualizedInfo
        val firstMessage = questMessageRepository.findById(data.questDialog.dialogHistory.first()).get()

        messageSenderService.sendMessage(
            MessageParams(
                chatId = data.userActualizedInfo.tui,
                text = "Кажется, ты хотел(-а) ответить на следующее сообщение:",
            ),
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = data.userActualizedInfo.tui,
                fromChatId = questionAuthor.tui.toString(),
                messageId = firstMessage.messageId!!,
            ),
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = data.userActualizedInfo.tui,
                text = "Ты можешь начать анонимный диалог с пользователем:",
                replyMarkup = createChooseKeyboard(data.questDialog),
            ),
        )

        return data.userActualizedInfo
    }

    private fun clickBan(data: RequestData): UserActualizedInfo {
        if (data.questDialog.questionStatus == QuestionStatus.CLOSED) return data.userActualizedInfo

        val questionAuthor = userRepository.findActiveUsersById(data.questDialog.authorId!!)!!

        if (data.userActualizedInfo.tui == questionAuthor.tui){

            val answerCallbackQuery = AnswerCallbackQuery().also {
                it.callbackQueryId = data.update.callbackQuery.id
                it.text = "\uD83D\uDEAB Вы не можете забанить себя"
            }
            bot.execute(answerCallbackQuery)

            return data.userActualizedInfo
        }

        questDialogRepository.save(
            data.questDialog.copy(
                questionStatus = QuestionStatus.CLOSED,
                finishTime = updatesUtil.getDate(data.update)
                    ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() }
            ),
        )

        questSegmentRepository.save(
            data.questSegment.copy(
                responderId = data.userActualizedInfo.id,
                finishTime = updatesUtil.getDate(data.update)
                    ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() }
            )
        )

        // TODO: логика банов скоро изменится, тут тоже надо будет менять код
        val authorInBan =
            userRepository.findActiveUsersById(data.questDialog.authorId)!!.copy(
                roles = mutableSetOf(UserRole.BANNED),
            )
        userRepository.save(authorInBan)

        val newText = "\uD83D\uDD34 Автор забанен пользователем ${MessageSenderUtil.userName(
            data.userActualizedInfo.lastTgNick,
            data.userActualizedInfo.fullName,
        )}."
        messageSenderService.editMessage(
            MessageParams(
                chatId = QUEST_RESPONDENT_CHAT_ID,
                messageId = data.questDialog.consoleMessageId?.toInt(),
                text = newText,
                replyMarkup = createUnbanKeyboard(data.questDialog),
            ),
        )

        return data.userActualizedInfo
    }

    private fun createChooseKeyboard(questDialog: QuestDialog) =
        createKeyboard(
            listOf(
                listOf(
                    InlineKeyboardButton("\uD83D\uDCAC Начать диалог")
                        .also { it.callbackData = QUEST_START_DIALOG.format(questDialog.id) },
                ),
            ),
        )

    private fun createUnbanKeyboard(questDialog: QuestDialog) =
        createKeyboard(
            listOf(
                listOf(
                    InlineKeyboardButton("Разбанить (doesn't work)")
                        // TODO("разбан еще не реализован")
                        .also { it.callbackData = QUEST_BAN.format(questDialog.id) },
                ),
            ),
        )

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun getQuestByCallbackData(callbackData: String): QuestDialog? {
        val questId = parseQuestId(callbackData)
        return questDialogRepository.findById(questId).getOrNull()
    }

    private fun parseQuestId(callbackData: String): Long {
        val questId =
            try {
                callbackData.split("|")[1].toLong()
            } catch (e: NumberFormatException) {
                throw NumberFormatException(
                    "Error during parse callBackData in questButtonHandler fetcher. " +
                        "Callback data: '$callbackData' has incorrect format. Correct format: 'something|{LONG}'",
                )
            }
        return questId
    }

    private data class RequestData(
        val questDialog: QuestDialog,
        val questSegment: QuestSegment,
        val userActualizedInfo: UserActualizedInfo,
        val update: Update,
    )
}
